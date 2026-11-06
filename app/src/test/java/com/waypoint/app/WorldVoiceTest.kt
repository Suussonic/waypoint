package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test
import uniffi.ferrostar.SpokenInstruction
import java.util.UUID

class WorldVoiceTest {
    private fun cue(text:String="In 200 metres, turn left onto King's Road.",distance:Double=200.0)=
        SpokenInstruction(text,"<speak>$text</speak>",distance,UUID.randomUUID())

    @Test fun everyWorldPreservesTheUsefulInstructionAndItsTimingIdentity() {
        for(world in World.entries) {
            val original=cue()
            val spoken=WorldVoiceNarrator(personaStyling=false) { 0 }.decorate(original,world)
            assertTrue(world.name,spoken.text.startsWith(original.text))
            assertTrue(spoken.text.endsWith(world.voiceStyle().opening))
            assertEquals(original.utteranceId,spoken.utteranceId)
            assertEquals(original.triggerDistanceBeforeManeuver,spoken.triggerDistanceBeforeManeuver,0.0)
            assertEquals("In 200 metres, turn left onto King's Road.",original.text)
            assertNotNull(original.ssml)
            assertNull(spoken.ssml)
            assertTrue(world.name,world.voiceStyle().pitch in .89f..1.14f)
            assertTrue(world.name,world.voiceStyle().pace in .91f..1.09f)
        }
    }
    @Test fun eachWorldHasItsOwnBoundedSpeechTone() {
        val styles=World.entries.map { world -> world.voiceStyle().let { it.pitch to it.pace } }
        assertEquals(World.entries.size,styles.distinct().size)
        assertTrue(styles.all { (pitch,pace) -> pitch in .89f..1.14f && pace in .91f..1.09f })
    }
    @Test fun imminentAndUrgentInstructionsHaveNoExtraWords() {
        val narrator=WorldVoiceNarrator(personaStyling=false) { 0 }
        for(original in listOf(cue(distance=25.0),cue(distance=Double.NaN),cue("Turn right now.",200.0),
            cue("Recalculating your route.",300.0))) {
            assertSame(original,narrator.decorate(original,World.SEA_CHART))
        }
    }
    @Test fun flavourIsSpacedAndResetForANewTrip() {
        var clock=0L
        val narrator=WorldVoiceNarrator(personaStyling=false) { clock }
        val lines=World.OLD_WEST.voiceLines()
        assertTrue(narrator.decorate(cue(),World.OLD_WEST).text.endsWith(lines.openings[0]))
        clock=44_999
        val repeated=cue("In 300 metres, turn right onto Left Bank Road.",300.0)
        assertSame(repeated,narrator.decorate(repeated,World.OLD_WEST))
        clock=45_000
        assertTrue(narrator.decorate(repeated,World.OLD_WEST).text.endsWith(lines.cues[0]))
        narrator.reset()
        assertTrue(narrator.decorate(repeated,World.OLD_WEST).text.endsWith(lines.openings[1]))
    }
    @Test fun standardGuidanceIsUnchangedAndWorldSwitchUsesTheNextProfile() {
        var clock=0L
        val narrator=WorldVoiceNarrator(personaStyling=false) { clock }
        val original=cue()
        assertSame(original,narrator.decorate(original,null))
        narrator.decorate(original,World.OLD_WEST)
        clock=60_000
        assertTrue(narrator.decorate(original,World.STALKER).text.endsWith(World.STALKER.voiceLines().cues[0]))
    }
    @Test fun ArrivalKeepsItsDestinationAndDoesNotClaimTaskCompletion() {
        val original=cue("You have arrived at your destination, City Library.",0.0)
        for(world in World.entries) {
            val spoken=WorldVoiceNarrator(personaStyling=false) { 0 }.decorate(original,world)
            assertTrue(spoken.text.startsWith(original.text))
            assertTrue(spoken.text.endsWith(world.voiceStyle().arrival))
            assertFalse(spoken.text.contains("quest complete",ignoreCase=true))
        }
    }

    @Test fun everyWorldRotatesThreeLinesForEachMomentWithoutChangingTheTurn() {
        for(world in World.entries) {
            var clock=0L
            val narrator=WorldVoiceNarrator(personaStyling=false) { clock }
            val lines=world.voiceLines()
            for(group in listOf(lines.openings,lines.cues,lines.arrivals)) {
                assertEquals(world.name,3,group.size)
                assertEquals(world.name,3,group.distinct().size)
                assertTrue(group.all { it.isNotBlank() && it.length<=80 })
            }
            val instruction=cue()
            val openings=(0..2).map {
                narrator.reset()
                narrator.decorate(instruction,world).text.removePrefix(instruction.text+" ")
            }
            assertEquals(lines.openings,openings)
            val cues=(0..2).map {
                clock+=45_000
                val spoken=narrator.decorate(instruction,world)
                assertEquals(instruction.utteranceId,spoken.utteranceId)
                spoken.text.removePrefix(instruction.text+" ")
            }
            assertEquals(lines.cues,cues)
            val arrived=cue("You have arrived at City Library.",0.0)
            assertEquals(lines.arrivals,(0..2).map { narrator.decorate(arrived,world).text.removePrefix(arrived.text+" ") })
            clock+=45_000
            assertTrue(narrator.decorate(instruction,world).text.endsWith(lines.cues.first()))
        }
    }
}
