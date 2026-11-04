package com.waypoint.app

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class NavigationSharingTransportTest {
    @Test fun cancellationCancelsTheActualHttpCall()=runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val call=OkHttpClient().newCall(Request.Builder().url(server.url("/shared")).head().build())
            val pending=async(Dispatchers.IO) { sharingResponseHeaders(call) }
            try {
                assertNotNull(server.takeRequest(5,TimeUnit.SECONDS))
                withTimeout(2000) { pending.cancelAndJoin() }
                assertTrue(call.isCanceled())
            } finally { pending.cancelAndJoin();call.cancel() }
        }
    }
    @Test fun returnsRedirectMetadataWithoutFollowingTheTarget()=runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location","https://example.invalid/untrusted"))
            val client=OkHttpClient.Builder().followRedirects(false).build()
            val headers=sharingResponseHeaders(client.newCall(Request.Builder().url(server.url("/shared")).head().build()))
            assertEquals(302,headers.first)
            assertEquals("https://example.invalid/untrusted",headers.second)
            assertEquals(1,server.requestCount)
        }
    }
}
