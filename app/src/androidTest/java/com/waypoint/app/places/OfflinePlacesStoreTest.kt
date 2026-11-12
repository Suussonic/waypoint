package com.waypoint.app.places

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.waypoint.app.Coordinate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OfflinePlacesStoreTest {
    private lateinit var temporaryFiles: File
    private lateinit var context: Context

    @Before fun setUp() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        temporaryFiles = File(target.cacheDir, "offline-places-${UUID.randomUUID()}").apply { check(mkdirs()) }
        context = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = temporaryFiles
        }
    }

    @After fun tearDown() {
        temporaryFiles.deleteRecursively()
    }

    @Test fun savedAreaSupportsOfflineNearbyAndNameSearchThenDeletesCleanly() = runBlocking {
        val center = Coordinate(0.0, 0.0)
        val store = OfflinePlacesStore(context)
        val inArea = NearbyPlace(
            id = "test-cafe", name = "Café Sample", point = Coordinate(0.001, 0.001),
            kind = "cafe", group = "food", address = "1 Example Road", phone = null,
            website = null, hours = null, origin = "OpenStreetMap", prominence = 1.0,
        )
        val outside = inArea.copy(id = "outside", name = "Outside Place", point = Coordinate(2.0, 2.0))
        val bounds = OfflinePlacesStore.Bounds(-0.1, -0.1, 0.1, 0.1)

        val token = requireNotNull(store.beginSave(42L))
        store.save(42L, token, bounds, listOf(inArea, outside))

        val reloaded = OfflinePlacesStore(context)
        assertTrue(reloaded.contains(42L))
        assertEquals(listOf("test-cafe"), reloaded.nearby(center, NearbyCategory.FOOD)?.map { it.id })
        assertEquals(listOf("Café Sample"), reloaded.search("cafe", center).map { it.name })
        assertEquals("A downloaded place remains searchable from outside its region without network access.",
            listOf("Café Sample"), reloaded.search("cafe", Coordinate(40.0, 40.0)).map { it.name })

        var upstreamCalls = 0
        val offlineFirst = OfflineFirstPlaceSearchProvider(context) { _, _ ->
            upstreamCalls += 1
            emptyList()
        }
        assertEquals(listOf("Café Sample"), offlineFirst.find("cafe", Coordinate(40.0, 40.0)).map { it.name })
        assertEquals("A local match is returned without invoking the online search provider.", 0, upstreamCalls)

        reloaded.beginDelete(42L)
        assertFalse(reloaded.contains(42L))
        assertNull(reloaded.nearby(center, NearbyCategory.FOOD))
        assertTrue(reloaded.search("cafe", center).isEmpty())
        reloaded.deleteFailed(42L)
        assertTrue(reloaded.contains(42L))

        reloaded.remove(42L)
        assertFalse(reloaded.contains(42L))
        assertNull(reloaded.nearby(center, NearbyCategory.FOOD))
    }

    @Test fun deleteInvalidatesAPlaceSavePausedBeforeItsCommit() = runBlocking {
        val regionId = 43L
        val center = Coordinate(0.0, 0.0)
        val bounds = OfflinePlacesStore.Bounds(-0.1, -0.1, 0.1, 0.1)
        val place = NearbyPlace(
            id = "race-cafe", name = "Race Café", point = Coordinate(0.001, 0.001),
            kind = "cafe", group = "food", address = "1 Example Road", phone = null,
            website = null, hours = null, origin = "OpenStreetMap", prominence = 1.0,
        )
        val commitReady = CompletableDeferred<Unit>()
        val allowCommit = CompletableDeferred<Unit>()
        val writer = OfflinePlacesStore(context) {
            commitReady.complete(Unit)
            allowCommit.await()
        }
        val deleter = OfflinePlacesStore(context)
        val token = requireNotNull(writer.beginSave(regionId))

        val save = async { runCatching { writer.save(regionId, token, bounds, listOf(place)) } }
        commitReady.await()
        deleter.beginDelete(regionId)
        deleter.remove(regionId)
        allowCommit.complete(Unit)

        val result = save.await()
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertFalse(deleter.contains(regionId))
        assertNull(deleter.nearby(center, NearbyCategory.FOOD))
        assertTrue(deleter.search("Race Café", center).isEmpty())
    }
}
