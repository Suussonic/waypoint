package com.waypoint.app

import androidx.car.app.CarContext
import androidx.car.app.Screen
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

/** Short, host-rendered lists keep destination selection usable with touch and rotary input. */
internal class WaypointCarDestinationsScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation

    override fun onGetTemplate(): Template {
        val places = (controller.state.value.saved + controller.state.value.recent)
            .distinctBy { it.id }.take(20)
        if (places.isEmpty()) return MessageTemplate.Builder("Save a place on your phone to use it in the car.")
            .setHeader(carHeader("Destinations")).build()
        val list = ItemList.Builder()
        places.forEach { place ->
            list.addItem(Row.Builder().setTitle(place.name)
                .addText(place.address.ifBlank { "Saved destination" })
                .setOnClickListener {
                    val current=controller.state.value
                    if(!acceptsCarInput() || current.navigating ||
                        (current.saved+current.recent).none { it==place })return@setOnClickListener
                    controller.select(place)
                    screenManager.pop()
                }.build())
        }
        return ListTemplate.Builder().setHeader(carHeader("Destinations"))
            .setSingleList(list.build()).build()
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
        state.searchResults.take(6).forEach { place ->
            list.addItem(Row.Builder().setTitle(place.name)
                .addText(place.address.ifBlank { "Place" })
                .setOnClickListener {
                    val current=controller.state.value
                    if(!acceptsCarInput() || current.navigating || current.query!=state.query ||
                        current.searchResults.none { it==place })return@setOnClickListener
                    controller.select(place)
                    screenManager.pop()
                }.build())
        }
        if (state.searchError != null) list.addItem(Row.Builder()
            .setTitle("Search unavailable")
            .addText("Check your connection and try again").build())
        return builder.setItemList(list.build()).build()
    }
}

internal class WaypointCarSettingsScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation

    override fun onGetTemplate(): Template {
        val state = controller.state.value
        val list = ItemList.Builder()
            .addItem(Row.Builder().setTitle("Map world")
                .addText("${state.world.title} · shared with your phone")
                .setOnClickListener { if(acceptsCarInput())screenManager.push(WaypointCarWorldScreen(carContext)) }
                .build())
            .addItem(Row.Builder().setTitle("Voice guidance")
                .addText(if (state.voice) "On" else "Muted")
                .setOnClickListener { if(acceptsCarInput()) { controller.toggleVoice(); invalidate() } }
                .build())
            .addItem(Row.Builder().setTitle("Distance units")
                .addText(if (state.imperial) "Miles and feet" else carContext.getString(R.string.metric_units))
                .setOnClickListener { if(acceptsCarInput()) { controller.toggleUnits(); invalidate() } }
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

internal class WaypointCarWorldScreen(context: CarContext) : Screen(context) {
    private val controller = (context.applicationContext as WaypointApplication).navigation

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        World.entries.forEach { world ->
            list.addItem(Row.Builder().setTitle(world.title)
                .setOnClickListener {
                    if(!acceptsCarInput())return@setOnClickListener
                    controller.changeWorld(world)
                    screenManager.pop()
                }.build())
        }
        return ListTemplate.Builder().setHeader(carHeader("Map world"))
            .setSingleList(list.build()).build()
    }
}
