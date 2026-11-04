package com.waypoint.app

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import uniffi.ferrostar.SpokenInstruction
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Requires an installed English TTS voice; speaks a synthetic instruction, never a real route. */
@RunWith(AndroidJUnit4::class)
class GuidanceVoiceDeviceTest {
    @Test fun installedEngineInitializesAndAcceptsNavigationSpeech() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val initialized = CountDownLatch(1)
        lateinit var voice: GuidanceVoice
        inst.runOnMainSync {
            voice = androidGuidanceVoice(inst.targetContext) {
                if (it != GuidanceVoiceStatus.STARTING && it != GuidanceVoiceStatus.IDLE) initialized.countDown()
            }
            voice.prepare()
        }
        try {
            assertTrue("Android TTS initialization timed out", initialized.await(20, TimeUnit.SECONDS))
            assertEquals(voice.status.problem, GuidanceVoiceStatus.READY, voice.status)
            inst.runOnMainSync {
                voice.onSpokenInstructionTrigger(SpokenInstruction(
                    "Waypoint voice guidance test. Turn left at the next street.", null, 35.0, UUID.randomUUID()))
            }
            SystemClock.sleep(6000)
            assertEquals("The engine rejected the test utterance", GuidanceVoiceStatus.READY, voice.status)
        } finally {
            inst.runOnMainSync { voice.shutdown() }
        }
    }
}
