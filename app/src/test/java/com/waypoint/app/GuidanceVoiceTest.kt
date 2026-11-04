package com.waypoint.app

import com.stadiamaps.ferrostar.core.SpokenInstructionObserver
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import uniffi.ferrostar.SpokenInstruction
import java.util.UUID

class GuidanceVoiceTest {
    private class Speaker : SpokenInstructionObserver {
        override val muteState = MutableStateFlow(false)
        val spoken = mutableListOf<String>()
        var stops = 0
        override fun setMuted(isMuted: Boolean) { muteState.value = isMuted }
        override fun stopAndClearQueue() { stops++ }
        override fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction) { spoken += spokenInstruction.text }
    }
    private class Fixture {
        val speaker = Speaker()
        var starts = 0
        var releases = 0
        var clock = 0L
        lateinit var complete: (GuidanceVoiceStatus) -> Unit
        val states = mutableListOf<GuidanceVoiceStatus>()
        val voice = GuidanceVoice(speaker, { starts++; complete = it }, { releases++ }, states::add, { clock })
        fun say(text: String) = voice.onSpokenInstructionTrigger(SpokenInstruction(text, null, 35.0, UUID.randomUUID()))
    }

    @Test fun explicitlyStartsAndDoesNotReportMissingDataDuringInitialization() {
        val f = Fixture()
        f.voice.prepare(); f.voice.prepare()
        assertEquals(1, f.starts)
        assertEquals(GuidanceVoiceStatus.STARTING, f.voice.status)
        assertNull(f.voice.status.problem)
        f.complete(GuidanceVoiceStatus.READY)
        f.say("Turn left")
        assertEquals(listOf("Turn left"), f.speaker.spoken)
    }
    @Test fun onlyLatestFreshInstructionSurvivesSlowStartup() {
        val f = Fixture(); f.voice.prepare()
        f.say("Turn left"); f.say("Turn right")
        f.complete(GuidanceVoiceStatus.READY)
        assertEquals(listOf("Turn right"), f.speaker.spoken)
        val stale = Fixture(); stale.voice.prepare(); stale.say("Old turn")
        stale.clock = 10_001; stale.complete(GuidanceVoiceStatus.READY)
        assertTrue(stale.speaker.spoken.isEmpty())
    }
    @Test fun muteAndEndTripClearPendingSpeech() {
        for (mute in listOf(false, true)) {
            val f = Fixture(); f.voice.prepare(); f.say("Old trip")
            if (mute) f.voice.setMuted(true) else f.voice.stopAndClearQueue()
            f.complete(GuidanceVoiceStatus.READY)
            assertTrue(f.speaker.spoken.isEmpty())
            assertEquals(1, f.speaker.stops)
        }
    }
    @Test fun failedEngineRetriesOnUnmuteAndIgnoresOldCallback() {
        val f = Fixture(); f.voice.prepare()
        val old = f.complete
        old(GuidanceVoiceStatus.ENGINE_UNAVAILABLE)
        assertNotNull(f.voice.status.problem)
        f.voice.setMuted(true); f.voice.setMuted(false)
        assertEquals(2, f.starts)
        old(GuidanceVoiceStatus.READY)
        assertEquals(GuidanceVoiceStatus.STARTING, f.voice.status)
        f.complete(GuidanceVoiceStatus.READY); f.say("New trip")
        assertEquals(listOf("New trip"), f.speaker.spoken)
    }
    @Test fun playbackFailureIsReportedAndCanRetry() {
        val f = Fixture(); f.voice.prepare(); f.complete(GuidanceVoiceStatus.READY)
        f.complete(GuidanceVoiceStatus.PLAYBACK_FAILED)
        assertEquals(GuidanceVoiceStatus.PLAYBACK_FAILED, f.states.last())
        f.voice.prepare()
        assertEquals(2, f.starts)
    }
    @Test fun shutdownCannotBeRevivedByLateInitialization() {
        val f = Fixture(); f.voice.prepare(); f.say("Turn left")
        f.voice.shutdown(); f.complete(GuidanceVoiceStatus.READY); f.voice.prepare()
        assertEquals(1, f.starts); assertEquals(1, f.releases)
        assertEquals(GuidanceVoiceStatus.IDLE, f.voice.status)
        assertTrue(f.speaker.spoken.isEmpty())
    }
}
