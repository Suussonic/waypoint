package com.waypoint.app

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Synthetic private-preference fixtures; no user query or location belongs in source. */
class SearchAliasStorageTest {
    @Test fun storedAliasReachesSearchWithoutBundlingPersonalData() = fixture(
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
        var requested=""
        val repository=object:NavigationRepository {
            override suspend fun search(query:String,near:Coordinate):List<Place> {
                requested=query
                return listOf(Place("fixture",query,"Example address",near))
            }
            override suspend fun reverse(point:Coordinate)="Demonstration"
            override suspend fun route(from:Coordinate,to:Coordinate,mode:TravelMode):RoadRoute = error("Unexpected route request")
        }
        lateinit var controller:NavigationController
        inst.runOnMainSync { controller=NavigationController(app,repository,"private-alias-test",null) }
        fun field(name:String)=controller.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(controller)
        try {
            val search=field("placeSearch") as PlaceSearch
            val result=runBlocking { search.search("Local nickname",Coordinate(51.5,-.12)) }
            assertEquals(expected,requested)
            assertEquals(expected,result.places.single().name)
        } finally {
            inst.runOnMainSync {
                (field("scope") as CoroutineScope).cancel()
                (field("guidance") as FerrostarGuidance).shutdown()
            }
            prefs.edit().clear().commit()
        }
    }
}
