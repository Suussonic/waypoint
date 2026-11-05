package com.waypoint.app

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MapRepositoryTest {
    private lateinit var server:MockWebServer
    private lateinit var repository:MapRepository
    @Before fun setup(){server=MockWebServer();server.start();repository=MapRepository(server.url("/").toString(),server.url("/").toString(),server.url("/").toString(),{_,_,_->null})}
    @After fun close(){server.shutdown()}
    private fun response(body:String,code:Int=200){server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type","application/json"))}
    private val feature="""{"properties":{"osm_type":"N","osm_id":1,"name":"River Cafe","street":"Example Road","city":"Town"},"geometry":{"type":"Point","coordinates":[-0.12,51.5]}}"""
    private val route="""{"code":"Ok","routes":[{"distance":111.0,"duration":60.0,"geometry":"???o}@","legs":[{"steps":[{"name":"Main Street","distance":111.0,"duration":60.0,"maneuver":{"type":"depart","location":[0.0,0.0]}},{"name":"","distance":0.0,"duration":0.0,"maneuver":{"type":"arrive","location":[0.001,0.0]}}]}]}]}"""
    @Test fun searchEncodesQueryAndKeepsCoordinateOrder()=runBlocking {
        response("""{"features":[$feature]}""")
        val places=repository.search("Cafe & Park",Coordinate(51.5,-0.12))
        assertEquals(Coordinate(51.5,-0.12),places.single().point)
        val request=server.takeRequest().requestUrl!!
        assertEquals("Cafe & Park",request.queryParameter("q"));assertEquals("51.5",request.queryParameter("lat"));assertEquals("-0.12",request.queryParameter("lon"))
    }
    @Test fun duplicateSearchResultsAreRemoved()=runBlocking {
        response("""{"features":[$feature,$feature]}""");assertEquals(1,repository.search("Cafe",Coordinate(0.0,0.0)).size)
    }
    @Test fun namelessSearchUsesStreet()=runBlocking {
        response("""{"features":[${feature.replace("River Cafe","")}]}""");assertEquals("Example Road",repository.search("Example",Coordinate(0.0,0.0)).single().name)
    }
    @Test fun reverseEmptyResponseHasUsefulFallback()=runBlocking {
        response("""{"features":[]}""");assertEquals("Your location",repository.reverse(Coordinate(0.0,0.0)))
    }
    @Test fun reversePlaceNamesNearbyDestinationWithoutMovingThePin()=runBlocking {
        response("""{"features":[{"properties":{"name":"Public Library","street":"Main Street","city":"Town","country":"UK"},"geometry":{"type":"Point","coordinates":[-0.1201,51.5001]}}]}""")
        val destination=Coordinate(51.5,-0.12)
        val place=repository.reversePlace(destination)!!
        assertEquals("Public Library",place.name)
        assertEquals("Main Street, Town, UK",place.address)
        assertEquals(destination,place.point)
    }
    @Test fun rateLimitIsActionable()=runBlocking {
        response("{}",429);val e=runCatching{repository.search("Cafe",Coordinate(0.0,0.0))}.exceptionOrNull()
        assertTrue(e?.message?.contains("busy")==true)
    }
    @Test fun serverErrorIsActionable()=runBlocking {
        response("{}",503);val e=runCatching{repository.search("Cafe",Coordinate(0.0,0.0))}.exceptionOrNull()
        assertTrue(e?.message?.contains("503")==true)
    }
    @Test fun routeParsesGeometryAndManeuvers()=runBlocking {
        response(route);val r=repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001))
        assertEquals(2,r.points.size);assertEquals(111.0,r.meters,0.1);assertEquals("arrive",r.steps.last().type)
        assertEquals(Coordinate(0.0,0.001),r.points.last());assertTrue(server.takeRequest().path!!.contains("geometries=polyline6"))
    }
    @Test fun retainsRightUTurnAcrossNorthInsteadOfAlwaysShowingLeft()=runBlocking {
        // Bearings from the public Trafalgar Square reproduction. The provider's
        // generic driving_side is not allowed to override its actual turn arc.
        val payload=route.replace("\"type\":\"depart\"", "\"type\":\"continue\",\"modifier\":\"uturn\",\"bearing_before\":347,\"bearing_after\":53")
        response(payload)
        val r=repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001))
        assertTrue(r.steps.first().uTurnRight)
        assertEquals("Make a U-turn onto Main Street",r.steps.first().instruction)
    }
    @Test fun ferrostarPayloadContainsRequiredBannerComponents()=runBlocking {
        response(route)
        val repo=MapRepository(server.url("/").toString(),server.url("/").toString(),server.url("/").toString(),
            { json,_,_ ->
                val first=json.getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(0)
                val primary=first.getJSONArray("bannerInstructions").getJSONObject(0).getJSONObject("primary")
                assertEquals("text",primary.getJSONArray("components").getJSONObject(0).getString("type"))
                assertTrue(first.getJSONArray("voiceInstructions").length()>0)
                null
            })
        val result=repo.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001))
        assertEquals(2,result.steps.size)
    }
    @Test fun noRouteIsNotTreatedAsSuccess()=runBlocking {
        response("""{"code":"NoRoute"}""");val e=runCatching{repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001))}.exceptionOrNull()
        assertTrue(e?.message?.contains("No driving route")==true)
    }
    @Test fun distantRoadSnapIsRejected()=runBlocking {
        response(route);val e=runCatching{repository.route(Coordinate(3.0,3.0),Coordinate(0.0,0.001))}.exceptionOrNull()
        assertTrue(e?.message?.contains("nearby road access")==true)
    }
    @Test fun malformedResponseFailsInsteadOfShowingFakeRoute()=runBlocking {
        response("not json");assertTrue(runCatching{repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001))}.isFailure)
    }
    @Test fun motorcycleUsesDedicatedCosting()=runBlocking {
        response(route);val r=repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001),TravelMode.MOTORCYCLE)
        val payload=org.json.JSONObject(server.takeRequest().requestUrl!!.queryParameter("json")!!)
        assertEquals("motorcycle",payload.getString("costing"));assertEquals("polyline6",payload.getString("shape_format"));assertEquals(TravelMode.MOTORCYCLE,r.mode)
    }
    @Test fun walkingUsesPedestrianCosting()=runBlocking {
        response(route);val r=repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001),TravelMode.WALK)
        val payload=org.json.JSONObject(server.takeRequest().requestUrl!!.queryParameter("json")!!)
        assertEquals("pedestrian",payload.getString("costing"));assertEquals(TravelMode.WALK,r.mode)
    }
    @Test fun rejectsOversizedResponsesBeforeParsing()=runBlocking {
        response(" ".repeat(2*1024*1024+1))
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.exceptionOrNull()?.message?.contains("too large")==true)
    }
    @Test fun rejectsExcessiveSearchResults()=runBlocking {
        response("{\"features\":["+List(9){feature}.joinToString(",")+"]}")
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.isFailure)
    }
    @Test fun rejectsChunkedResponsesWithoutContentLength()=runBlocking {
        server.enqueue(MockResponse().setChunkedBody(" ".repeat(2*1024*1024+1),8192))
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.exceptionOrNull()?.message?.contains("too large")==true)
    }
    @Test fun limitsDecompressedBytesInsteadOfGzipSize()=runBlocking {
        val bytes=java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(bytes).use { it.write(" ".repeat(2*1024*1024+1).toByteArray()) }
        server.enqueue(MockResponse().setBody(okio.Buffer().write(bytes.toByteArray())).addHeader("Content-Encoding","gzip"))
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.exceptionOrNull()?.message?.contains("too large")==true)
    }
    @Test fun rejectsRouteGeometryBeyondPointBudget()=runBlocking {
        response(route.replace("???o}@","??".repeat(50_001)))
        assertTrue(runCatching { repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001)) }.exceptionOrNull()?.message?.contains("too short or too complex")==true)
    }
    @Test fun rejectsInvalidProviderCoordinates()=runBlocking {
        response("{\"features\":[${feature.replace("-0.12,51.5","-0.12,122.8")}]}")
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.isFailure)
    }
    @Test fun rejectsNegativeRouteDuration()=runBlocking {
        response(route.replace("\"duration\":60.0","\"duration\":-1.0"))
        assertTrue(runCatching { repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001)) }.isFailure)
    }
    @Test fun bicycleUsesCyclingProfileAndProviderDuration()=runBlocking {
        response(route)
        val result=repository.route(Coordinate(0.0,0.0),Coordinate(0.0,0.001),TravelMode.BICYCLE)
        val request=server.takeRequest()
        val payload=org.json.JSONObject(request.requestUrl!!.queryParameter("json")!!)
        assertEquals("bicycle",payload.getString("costing"))
        assertEquals("osrm",payload.getString("format"))
        assertEquals(TravelMode.BICYCLE,result.mode)
        assertEquals(60.0,result.seconds,0.0)
    }
    @Test fun refusesRedirectBeforeSendingCoordinatesElsewhere()=runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location",server.url("/unexpected")))
        assertTrue(runCatching { repository.search("Park",Coordinate(0.0,0.0)) }.isFailure)
        assertEquals(1,server.requestCount)
    }
    @Test fun cancellationDoesNotWaitForResponseDeadline()=runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val request=launch { repository.search("Park",Coordinate(0.0,0.0)) }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { assertNotNull(server.takeRequest(5,TimeUnit.SECONDS)) }
        withTimeout(1000) { request.cancelAndJoin() }
    }
}
