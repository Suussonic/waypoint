package com.waypoint.app

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.waypoint.app.offline.OfflineMapsPanel
import com.waypoint.app.offline.OfflineMapsState
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSettingsTest {
    @Test fun remainingScreensFollowTheAppLanguageWithoutTranslatingPlaceNames() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val device = UiDevice.getInstance(inst)
        val place = Place("locale-fixture", "Central station", "", Coordinate(48.85, 2.35))
        val planner = object : TransitPlanner {
            override suspend fun plan(from: Coordinate, to: Coordinate): List<TransitItinerary> = emptyList()
        }
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            var previous = LocaleListCompat.getEmptyLocaleList()
            scenario.onActivity { previous = AppCompatDelegate.getApplicationLocales() }
            var screen = 0
            fun show(content: @Composable () -> Unit) {
                val id = screen++
                scenario.onActivity { activity ->
                    activity.setContent {
                        key(id) { QuestTheme(World.NIGHT_CITY) { content() } }
                    }
                }
                inst.waitForIdleSync()
            }
            fun visible(text: String) {
                assertTrue("Missing localized screen text: $text", device.wait(Until.hasObject(By.text(text)), 10000))
            }
            try {
                for (language in listOf("fr", "en")) {
                    inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language)) }
                    inst.waitForIdleSync()
                    val french = language == "fr"
                    show { FeedbackSheet(World.NIGHT_CITY, {}, {}) }
                    visible(if (french) "Envoyer un avis" else "Send feedback")
                    visible(if (french) "Signaler un bug" else "Bug report")
                    visible(if (french) "Proposer une fonctionnalité" else "Feature request")
                    show { OfflineMapsPanel(OfflineMapsState(), false, {}, {}, {}, {}, {}) }
                    visible(if (french) "Cartes hors ligne" else "Offline maps")
                    visible(if (french) "Aucune zone enregistrée" else "Nothing saved yet")
                    show {
                        NearbyPlacesSheet(
                            state = NavState(world = World.NIGHT_CITY), center = null,
                            googleEnabled = false, savedIds = emptySet(), onGoogleEnabledChange = {},
                            commonsPhotosEnabled = false, onCommonsPhotosEnabledChange = {},
                            onPlacesChanged = {}, onLocate = {}, onSelect = {}, onSave = {}, onClose = {},
                        )
                    }
                    visible(if (french) "Explorer les environs" else "Explore nearby")
                    assertTrue(device.wait(Until.hasObject(By.desc(if (french) "Fermer les lieux à proximité" else "Close nearby places")), 10000))
                    show { TransitSheet(place, null, World.NIGHT_CITY, planner) {} }
                    visible(if (french) "Bus et transports" else "Bus & transit")
                    visible(if (french) "Vers Central station" else "To Central station")
                    visible(if (french) "En attente de votre position. Activez le GPS pour prévoir un trajet en bus ou en train."
                        else "Waiting for your location. Turn on GPS to plan a bus or train trip.")
                    show {
                        DestinationCard(
                            NavState(world = World.NIGHT_CITY, selected = place, mode = TravelMode.MOTORCYCLE, routing = true),
                            {}, {}, {}, {}, {}, {}, {},
                        )
                    }
                    visible("Central station")
                    visible(if (french) "Recherche d’un itinéraire à moto…" else "Finding a motorcycle route…")
                    if (french) scenario.onActivity {
                        assertEquals("0 avis", it.resources.getQuantityString(R.plurals.nearby_reviews, 0, 0))
                        assertEquals("1 avis", it.resources.getQuantityString(R.plurals.nearby_reviews, 1, 1))
                        assertEquals("2 avis", it.resources.getQuantityString(R.plurals.nearby_reviews, 2, 2))
                        assertEquals("0 quête terminée", it.resources.getQuantityString(R.plurals.quest_celebration_count, 0, 0))
                    }
                }
            } finally {
                inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            }
        }
    }

    @Test fun journeyPageDisplaysFrenchIncludingUnlockedRewards() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val device = UiDevice.getInstance(inst)
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            var previous = LocaleListCompat.getEmptyLocaleList()
            scenario.onActivity { previous = AppCompatDelegate.getApplicationLocales() }
            try {
                inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr")) }
                inst.waitForIdleSync()
                val application = context.applicationContext as WaypointApplication
                fun show(snapshot: QuestSnapshot) {
                    scenario.onActivity { activity ->
                        activity.setContent {
                            androidx.compose.runtime.key(snapshot) {
                                QuestTheme(World.NIGHT_CITY) {
                                    QuestProfileScreen(World.NIGHT_CITY, snapshot, application.quests) {}
                                }
                            }
                        }
                    }
                    device.waitForIdle()
                    assertTrue(device.wait(Until.hasObject(By.text("Votre parcours")), 10000))
                    repeat(5) {
                        if (!device.hasObject(By.text("Un peu de progrès, chaque jour")))
                            device.findObject(By.scrollable(true))?.fling(Direction.UP)
                    }
                }
                fun find(text: String) {
                    repeat(10) {
                        if (device.hasObject(By.text(text))) return
                        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, .7f)
                    }
                    assertTrue("Missing French journey text: $text", device.hasObject(By.text(text)))
                }
                show(QuestSnapshot(loading = false))
                find("Un peu de progrès, chaque jour")
                find("Niveau 1 · 0 XP")
                find("Terminez votre première quête pour obtenir le badge « Premier pas ».")
                find("De nouveaux cadres aux niveaux 2, 5 et 10.")
                find("Disponible après 5 quêtes terminées. Vous en avez terminé 0 pour le moment.")
                find("Sur ce téléphone")
                show(QuestSnapshot(rewards = List(50) { QuestReward("locale-fixture-$it", 20, true) }, loading = false))
                find("Niveau 11 · 1000 XP")
                find("Premier pas\nTrouver son rythme\nAventurier aguerri\nDu temps bien employé")
                find("Guide des chemins")
                find("Tracer votre propre chemin")
                find("La route à venir")
            } finally {
                inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            }
        }
    }

    @Test fun settingsDropdownChangesLanguageInBothDirections() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        val device=UiDevice.getInstance(inst)
        val navigation=(context.applicationContext as WaypointApplication).navigation
        ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).setAction(UpdateAlerts.OPEN_UPDATES)).use { scenario ->
            var previous=LocaleListCompat.getEmptyLocaleList()
            scenario.onActivity { previous=AppCompatDelegate.getApplicationLocales() }
            try {
                inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
                val english=device.wait(Until.findObject(By.text("English")),10000)
                assertNotNull(english)
                english.click()
                val french=device.wait(Until.findObject(By.text("Français")),5000)
                assertNotNull(french)
                french.click()
                assertTrue(device.wait(Until.hasObject(By.text("Personnalisez votre expérience")),10000))
                scenario.recreate()
                scenario.onActivity {
                    assertEquals("fr",it.resources.configuration.locales[0].language)
                    assertEquals("Langue",it.getString(R.string.language_title))
                    assertEquals("Mises à jour",it.getString(R.string.update_title))
                    assertEquals("Carte",it.getString(R.string.nav_map))
                    assertEquals("Lieu ou adresse",it.getString(R.string.search_place_address))
                    assertEquals("Ajouter une quête",it.getString(R.string.quest_add))
                    assertEquals("Trouvez votre prochain monde",it.getString(R.string.world_find_title))
                    assertEquals("Rechercher un monde ou un jeu",it.getString(R.string.world_find_hint))
                    assertEquals("Favoris",it.getString(R.string.world_filter_favourites))
                    assertEquals("Modifier la quête",it.getString(R.string.quest_edit))
                    assertEquals("Aucun lieu enregistré",it.getString(R.string.saved_empty_title))
                    assertSame(navigation,(it.application as WaypointApplication).navigation)
                }
                val selected=device.wait(Until.findObject(By.text("Français")),5000)
                assertNotNull(selected)
                selected.click()
                val back=device.wait(Until.findObject(By.text("English")),5000)
                assertNotNull(back)
                back.click()
                assertTrue(device.wait(Until.hasObject(By.text("Make it your own")),10000))
                scenario.onActivity {
                    assertEquals("App updates",it.getString(R.string.update_title))
                    assertEquals("Map",it.getString(R.string.nav_map))
                    assertEquals("Place or address",it.getString(R.string.search_place_address))
                    assertEquals("Add a quest",it.getString(R.string.quest_add))
                    assertEquals("Find your next world",it.getString(R.string.world_find_title))
                    assertEquals("Find a world or game",it.getString(R.string.world_find_hint))
                    assertEquals("Favourites",it.getString(R.string.world_filter_favourites))
                    assertEquals("Edit quest",it.getString(R.string.quest_edit))
                }
            } finally {
                inst.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            }
        }
    }

}
