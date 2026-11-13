package com.waypoint.app

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test

/** Synthetic private-preference fixtures; no user query or location belongs in source. */
class SearchAliasStorageTest {
    @Test fun storedAliasIsReadFromPrivatePreferencesWithoutBundlingPersonalData() = fixture(
        """{"Local nickname":"Example Junction"}""", "Example Junction"
    )
    @Test fun corruptAliasStorageFallsBackToOriginalQuery() = fixture("not json", "Local nickname")
    @Test fun nonStringAliasStorageFallsBackToOriginalQuery() = fixture(
        """{"Local nickname":123}""", "Local nickname"
    )

    private fun fixture(stored:String,expected:String) {
        val inst=InstrumentationRegistry.getInstrumentation()
        val app=inst.targetContext.applicationContext as Application
        val prefs=app.getSharedPreferences("private-alias-test",0)
        prefs.edit().clear().putString("searchAliases",stored).putBoolean("voice",false).commit()
        val repository=object:NavigationRepository {
            override suspend fun search(query:String,near:Coordinate):List<Place> = emptyList()
            override suspend fun reverse(point:Coordinate)="Demonstration"
            override suspend fun route(from:Coordinate,to:Coordinate,mode:TravelMode):RoadRoute = error("Unexpected route request")
        }
        lateinit var controller:NavigationController
        inst.runOnMainSync { controller=NavigationController(app,repository,"private-alias-test",null) }
        fun field(name:String)=controller.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(controller)
        try {
            val reader=controller.javaClass.getDeclaredMethod("readSearchAliases").apply{isAccessible=true}
            @Suppress("UNCHECKED_CAST")
            val aliases=reader.invoke(controller) as Map<String,String>
            assertEquals(expected,SearchAliases(aliases).resolve("Local nickname"))
            if(expected=="Example Junction") assertEquals(mapOf("Local nickname" to expected),aliases)
            else assertTrue("Malformed and non-string preference values must be ignored",aliases.isEmpty())
        } finally {
            inst.runOnMainSync {
                (field("scope") as CoroutineScope).cancel()
                (field("guidance") as FerrostarGuidance).shutdown()
            }
            prefs.edit().clear().commit()
        }
    }
}
