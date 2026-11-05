package com.waypoint.app

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PlaceSearchTest {
    private val near=Coordinate(51.50,-.12)
    private val station=Place("station","Riverton","Riverton, Example District",Coordinate(51.501,-.121),"geocoder")
    @Test fun deletedLocalMatchesDoNotSurviveInTheProviderCache()=runBlocking {
        var calls=0
        val search=PlaceSearch(PlaceSearchProvider { _,_->calls++;emptyList() })
        assertEquals(listOf(station),search.search("Riverton",near,listOf(station)).places)
        assertTrue(search.search("Riverton",near).places.isEmpty())
        assertEquals("Remote cache should still be reused",1,calls)
    }
    @Test fun railwayQueryRanksTheLocalStationOverUnrelatedStations() {
        val unrelated=Place("other","Farvale Railway Station","Farvale, Example Country",Coordinate(51.6,-.2),"station")
        val farmland=Place("farm","Riverton Research Centre","Riverton",near,"farmland")
        assertEquals(listOf(station),PlaceMatcher.ranked("Riverton Railway Station",near,listOf(unrelated,farmland,station)))
    }
    @Test fun namesIgnoreCaseDiacriticsAndSpacing() {
        val p=Place("cafe","Café Monde","Paris",Coordinate(48.85,2.3))
        assertEquals(listOf(p),PlaceMatcher.ranked("cafe monde",near,listOf(p)))
    }
    @Test fun spellingErrorsAndJoinedNamesStillMatch() {
        val place=Place("junction","Willow More","Riverton",near)
        assertTrue(PlaceMatcher.score("Willovmore",place)>.7)
        assertEquals(listOf(place),PlaceMatcher.ranked("Willovmore",near,listOf(place)))
        assertEquals(listOf("Willovmore","Willov more"),PlaceMatcher.variants("Willovmore"))
    }
    @Test fun duplicateCoordinatesFromTwoProvidersAreMerged() {
        assertEquals(1,PlaceMatcher.ranked("Riverton",near,listOf(station,station.copy(id="second"))).size)
    }
    @Test fun fallbackRunsWhenPrimaryMissesStation()=runBlocking {
        var fallbackCalls=0
        val search=PlaceSearch(PlaceSearchProvider{_,_->emptyList()},PlaceSearchProvider{_,_->fallbackCalls++;listOf(station)})
        assertEquals(listOf(station),search.search("Riverton Railway Station",near).places)
        assertEquals(1,fallbackCalls)
        search.search("Riverton Railway Station",near)
        assertEquals(1,fallbackCalls)
    }
    @Test fun primaryOutageDoesNotHideFallbackOrSavedPlaces()=runBlocking {
        val search=PlaceSearch(PlaceSearchProvider{_,_->throw IOException()},PlaceSearchProvider{_,_->listOf(station)})
        val result=search.search("Riverton Railway Station",near)
        assertEquals(listOf(station),result.places);assertNotNull(result.note)
    }
    @Test fun cancellationStopsBeforeFallbackAndDoesNotCache()=runBlocking {
        var fallbackCalls=0
        val entered=CompletableDeferred<Unit>()
        val search=PlaceSearch(PlaceSearchProvider{_,_->entered.complete(Unit);delay(60000);emptyList()},PlaceSearchProvider{_,_->fallbackCalls++;emptyList()})
        val job=launch{search.search("Riverton",near)}
        entered.await();job.cancelAndJoin();assertEquals(0,fallbackCalls)
    }
    @Test fun strongLocalResultRemovesUnrelatedFuzzyFillers() {
        val exact=Place("junction","Willow More","Riverton",near)
        val unrelated=Place("shop","Wilma Mode","Rotterdam",Coordinate(51.9,4.4),"clothes")
        assertEquals(listOf(exact),PlaceMatcher.ranked("Willow More",near,listOf(unrelated,exact)))
    }
    @Test fun categoryShortcutsStillReturnNamedBusinesses() {
        val fuel=Place("fuel","Example Petroleum","Main Road",near,"fuel")
        val cafe=Place("cafe","Monde","Main Road",near,"cafe")
        assertEquals(listOf(fuel),PlaceMatcher.ranked("fuel",near,listOf(fuel)))
        assertEquals(listOf(cafe),PlaceMatcher.ranked("coffee",near,listOf(cafe)))
    }
    @Test fun confirmedAliasIsSentToProviderWithoutChangingReturnedPoint()=runBlocking {
        var received=""
        val result=Place("junction","Willow More","Provider address",near,"geocoder")
        val search=PlaceSearch(PlaceSearchProvider{_,_->emptyList()},PlaceSearchProvider{q,_->received=q;listOf(result)},SearchAliases(mapOf("Willovmore" to "Willow More")))
        val found=search.search("Willovmore",near)
        assertEquals("Willow More",received);assertEquals(result,found.places.single());assertTrue(found.note!!.contains("Willow More"))
    }
    @Test fun aliasesAreEmptyByDefaultAndPrivateToEachInstance() {
        assertEquals("Local nickname",SearchAliases().resolve("Local nickname"))
        val configured=SearchAliases(mapOf("Local nickname" to "Example Junction"))
        assertEquals("Example Junction",configured.resolve("LOCAL NICKNAME"))
        assertEquals("Local nickname",SearchAliases().resolve("Local nickname"))
    }
    @Test fun invalidAliasesAreIgnored() {
        val aliases=SearchAliases(mapOf("" to "Invalid","Long" to "a".repeat(257),"Control" to "bad\nname"))
        assertEquals("Long",aliases.resolve("Long"))
        assertEquals("Control",aliases.resolve("Control"))
    }
    @Test fun excessiveInputDoesNotReachProviders()=runBlocking {
        var calls=0
        val search=PlaceSearch(PlaceSearchProvider{_,_->calls++;emptyList()})
        assertTrue(runCatching{search.search("a".repeat(257),near)}.isFailure);assertEquals(0,calls)
    }
    @Test fun secondCoordinateSearchFindsItsOwnDestinationWithoutCallingProviders()=runBlocking {
        var calls=0
        val first=Place("coordinate:51.5,-0.12","First place","Old coordinates",near)
        val search=PlaceSearch(PlaceSearchProvider{_,_->calls++;listOf(first)})
        val second=search.search("51.5012, -0.1234",near,listOf(first)).places.single()
        assertEquals(Coordinate(51.5012,-0.1234),second.point)
        assertNotEquals(first.id,second.id)
        assertEquals(0,calls)
        assertEquals(second,search.search("51.5012, -0.1234",near,listOf(first,second)).places.single())
    }
    @Test fun invalidCoordinatePairDoesNotFallBackToAnUnrelatedSavedPlace()=runBlocking {
        var calls=0
        val search=PlaceSearch(PlaceSearchProvider{_,_->calls++;listOf(station)})
        val result=search.search("91, 181",near,listOf(station))
        assertTrue(result.places.isEmpty())
        assertTrue(result.note!!.contains("valid coordinates"))
        assertEquals(0,calls)
    }
    @Test fun numberedStreetAddressStillUsesPlaceSearch()=runBlocking {
        var calls=0
        val search=PlaceSearch(PlaceSearchProvider{_,_->calls++;emptyList()})
        search.search("12, 34 High Street",near)
        assertEquals(1,calls)
    }
}
