package com.waypoint.app

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class GuidanceSessionGateTest {
    @Test fun queuedOldReplacementCannotMutateANewerTrip() {
        val gate=GuidanceSessionGate()
        var coreRoute="A"
        gate.transition { coreRoute="A" }
        val ticket=gate.read { it }
        // The dispatcher has captured A's replacement but has not executed it.
        gate.transition { coreRoute="stopped" }
        gate.transition { coreRoute="B" }
        assertFalse(gate.applyIfCurrent(ticket) { coreRoute="late replacement for A" })
        assertEquals("B",coreRoute)
    }
    @Test fun tripTransitionWaitsForAnAlreadyRunningSdkMutation() {
        val gate=GuidanceSessionGate()
        var coreRoute="A"
        gate.transition { coreRoute="A" }
        val ticket=gate.read { it }
        val mutationStarted=CountDownLatch(1)
        val releaseMutation=CountDownLatch(1)
        val transitionAttempted=CountDownLatch(1)
        val workers=Executors.newFixedThreadPool(2)
        try {
            val replacement=workers.submit<Boolean> {
                gate.applyIfCurrent(ticket) {
                    mutationStarted.countDown()
                    check(releaseMutation.await(3,TimeUnit.SECONDS))
                    coreRoute="replacement for A"
                }
            }
            assertTrue(mutationStarted.await(3,TimeUnit.SECONDS))
            val transition=workers.submit {
                transitionAttempted.countDown()
                gate.transition { coreRoute="B" }
            }
            assertTrue(transitionAttempted.await(3,TimeUnit.SECONDS))
            releaseMutation.countDown()
            assertTrue(replacement.get(3,TimeUnit.SECONDS))
            transition.get(3,TimeUnit.SECONDS)
            assertEquals("B",coreRoute)
            assertFalse(gate.applyIfCurrent(ticket) { coreRoute="stale" })
        } finally { releaseMutation.countDown();workers.shutdownNow() }
    }
    @Test fun failedMutationReleasesTheGateAndStopInvalidatesItsTicket() {
        val gate=GuidanceSessionGate()
        val ticket=gate.read { it }
        assertThrows(IllegalStateException::class.java) {
            gate.applyIfCurrent(ticket) { error("SDK failure") }
        }
        gate.transition { }
        assertFalse(gate.applyIfCurrent(ticket) { fail("Old mutation survived stop") })
    }
}
