package com.waypoint.app

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Toggle
import androidx.lifecycle.repeatOnLifecycle
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private fun carHeader(title: String) = Header.Builder()
    .setTitle(title).setStartHeaderAction(Action.BACK).build()
private fun Screen.acceptsCarInput() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
private fun boundedCarSearch(text: String) = text.take(256).filterNot(Char::isISOControl)

private fun Screen.observeCarState(key: (NavState) -> Any?) {
    val controller = (carContext.applicationContext as WaypointApplication).navigation
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) { invalidate() }
        override fun onDestroy(owner: LifecycleOwner) { scope.cancel() }
    })
    scope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.state.map(key).distinctUntilChanged().collect { invalidate() }
        }
    }
}

private fun CarContext.listLimit(): Int = getCarService(ConstraintManager::class.java)
    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST).coerceAtLeast(2)

/** Short, host-rendered lists keep every destination reachable with touch and rotary input. */
internal class WaypointCarDestinationsScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation
    init { observeCarState { it.saved to it.recent } }
    override fun onGetTemplate(): Template {
        val state = controller.state.value
        if(state.saved.isEmpty() && state.recent.isEmpty()) return MessageTemplate.Builder(carContext.getString(R.string.car_no_destinations))
            .setHeader(carHeader(carContext.getString(R.string.car_destinations)))
            .addAction(Action.Builder().setTitle(carContext.getString(R.string.car_search))
                .setOnClickListener { if(acceptsCarInput())screenManager.push(WaypointCarSearchScreen(carContext)) }.build()).build()
        val list = ItemList.Builder()
        listOf(true, false).forEach { saved ->
            val places = if(saved) state.saved else state.recent
            if(places.isNotEmpty()) list.addItem(Row.Builder()
                .setTitle(carContext.getString(if(saved)R.string.car_saved else R.string.car_recent))
                .addText(carContext.resources.getQuantityString(R.plurals.car_places,places.size,places.size))
                .setBrowsable(true)
                .setOnClickListener { if(acceptsCarInput())screenManager.push(WaypointCarPlacesScreen(carContext,saved)) }.build())
        }
        return ListTemplate.Builder().setHeader(carHeader(carContext.getString(R.string.car_destinations))).setSingleList(list.build()).build()
    }
}

internal class WaypointCarPlacesScreen(context: CarContext, private val saved: Boolean,
    private val allowedIds: Set<String>? = null) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation
    init { observeCarState { if(saved)it.saved else it.recent } }
    override fun onGetTemplate(): Template {
        val places = (if(saved)controller.state.value.saved else controller.state.value.recent)
            .distinctBy { it.id }.filter { allowedIds == null || it.id in allowedIds }
        val title = carContext.getString(if(saved)R.string.car_saved else R.string.car_recent)
        if(places.isEmpty()) return MessageTemplate.Builder(carContext.getString(R.string.car_places_removed)).setHeader(carHeader(title)).build()
        val list = ItemList.Builder()
        carMenuGroups(places,carContext.listLimit()).forEach { group ->
            val place = group.first()
            if(group.size > 1) list.addItem(Row.Builder().setTitle(place.name)
                .addText(carContext.getString(R.string.car_through_place,group.last().name))
                .setBrowsable(true).setOnClickListener {
                    if(acceptsCarInput())screenManager.push(WaypointCarPlacesScreen(carContext,saved,group.map { it.id }.toSet()))
                }.build())
            else list.addItem(Row.Builder().setTitle(place.name)
                .addText(place.address.ifBlank { carContext.getString(R.string.car_destination) })
                .setOnClickListener {
                    val current=controller.state.value
                    if(!acceptsCarInput() || current.navigating || (current.saved+current.recent).none { it==place })return@setOnClickListener
                    controller.select(place)
                    screenManager.popToRoot()
                }.build())
        }
        return ListTemplate.Builder().setHeader(carHeader(title)).setSingleList(list.build()).build()
    }
}

/** Android Auto supplies the keyboard and restricts typing while the car is moving. */
internal class WaypointCarSearchScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { scope.cancel() }
        })
        scope.launch {
            controller.state.map { Triple(it.searching, it.searchResults, it.searchError) }
                .distinctUntilChanged().collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val state = controller.state.value
        val builder = SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) {
                if(acceptsCarInput())controller.search(boundedCarSearch(searchText))
            }
            override fun onSearchSubmitted(searchText: String) {
                if(!acceptsCarInput())return
                controller.search(boundedCarSearch(searchText))
                controller.submitSearch()
            }
        }).setHeaderAction(Action.BACK)
            .setSearchHint("Search places")
            .setShowKeyboardByDefault(true)
        if (state.searching) return builder.setLoading(true).build()
        val list = ItemList.Builder()
        state.searchResults.take(carContext.listLimit()).forEach { place ->
            list.addItem(Row.Builder().setTitle(place.name)
                .addText(place.address.ifBlank { "Place" })
                .setOnClickListener {
                    val current=controller.state.value
                    if(!acceptsCarInput() || current.navigating || current.query!=state.query ||
                        current.searchResults.none { it==place })return@setOnClickListener
                    controller.select(place)
                    screenManager.popToRoot()
                }.build())
        }
        if(state.searchError != null && state.searchResults.isEmpty()) list.setNoItemsMessage(carContext.getString(R.string.car_search_error))
        else if(state.searchResults.isEmpty()) list.setNoItemsMessage(carContext.getString(
            if(state.query.isBlank())R.string.car_search_empty else R.string.car_search_no_results))
        return builder.setItemList(list.build()).build()
    }
}

internal class WaypointCarSettingsScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation
    init { observeCarState { Triple(it.world,it.voice,it.imperial) } }

    override fun onGetTemplate(): Template {
        val state = controller.state.value
        val list = ItemList.Builder()
            .addItem(Row.Builder().setTitle("Map world")
                .addText("${state.world.title} · shared with your phone")
                .setOnClickListener { if(acceptsCarInput())screenManager.push(WaypointCarWorldScreen(carContext)) }
                .build())
            .addItem(Row.Builder().setTitle("Voice guidance")
                .addText(if (state.voice) "On" else "Muted")
                .setToggle(Toggle.Builder { checked -> if(acceptsCarInput() && checked != controller.state.value.voice)controller.toggleVoice() }.setChecked(state.voice).build())
                .build())
            .addItem(Row.Builder().setTitle(carContext.getString(R.string.car_imperial))
                .addText(if (state.imperial) "Miles and feet" else carContext.getString(R.string.metric_units))
                .setToggle(Toggle.Builder { checked -> if(acceptsCarInput() && checked != controller.state.value.imperial)controller.toggleUnits() }.setChecked(state.imperial).build())
                .build())
            .addItem(Row.Builder().setTitle(carContext.getString(R.string.map_data_licenses))
                .addText(carContext.getString(R.string.license_sources))
                .setOnClickListener { if(acceptsCarInput())screenManager.push(WaypointCarCreditsScreen(carContext)) }
                .build())
            .build()
        return ListTemplate.Builder().setHeader(carHeader("Map settings"))
            .setSingleList(list).build()
    }
}

internal class WaypointCarCreditsScreen(context: CarContext) : Screen(context) {
    override fun onGetTemplate(): Template = MessageTemplate.Builder(
        "Map data: OpenStreetMap contributors (ODbL). Tiles: OpenFreeMap. " +
            carContext.getString(R.string.map_credits)
    ).setHeader(carHeader(carContext.getString(R.string.map_data_licenses))).build()
}

internal class WaypointCarWorldScreen(context: CarContext, private val groupName: Int? = null,
    private val subset: List<World>? = null) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation
    init { observeCarState { it.world } }
    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        if(groupName == null) {
            carWorldGroups.forEach { (name, worlds) ->
                list.addItem(Row.Builder().setTitle(carContext.getString(name))
                    .addText(if(controller.state.value.world in worlds)carContext.getString(R.string.car_group_current,controller.state.value.world.title)
                        else carContext.getString(R.string.car_group_examples,worlds[0].title,worlds[1].title))
                    .setBrowsable(true).setOnClickListener {
                        if(acceptsCarInput())screenManager.push(WaypointCarWorldScreen(carContext,name))
                    }.build())
            }
        } else carMenuGroups(subset ?: carWorldGroups.getValue(groupName),carContext.listLimit().coerceAtMost(6)).forEach { worlds ->
            if(worlds.size>1) {
                list.addItem(Row.Builder().setTitle("${worlds.first().title} – ${worlds.last().title}")
                    .addText(carContext.resources.getQuantityString(R.plurals.car_world_count,worlds.size,worlds.size))
                    .setBrowsable(true).setOnClickListener {
                        if(acceptsCarInput())screenManager.push(WaypointCarWorldScreen(carContext,groupName,worlds))
                    }.build())
            } else {
            val world=worlds.single()
            list.addItem(Row.Builder().setTitle(world.title)
                .addText(if(world == controller.state.value.world)carContext.getString(R.string.car_current_world) else world.inspiration())
                .setOnClickListener {
                    if(!acceptsCarInput())return@setOnClickListener
                    controller.changeWorld(world)
                    screenManager.popToRoot()
                }.build())
            }
        }
        return ListTemplate.Builder().setHeader(carHeader(carContext.getString(groupName ?: R.string.car_map_world)))
            .setSingleList(list.build()).build()
    }
}
