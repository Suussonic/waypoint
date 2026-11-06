package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldPersonaTest {
    private val samples = listOf(
        "In 200 meters, turn left onto Grand Trunk Road.",
        "Turn right onto Kalyani Expressway.",
        "Bear left onto Ishwar Gupta Setu.",
        "Continue straight for 1.4 kilometres.",
        "Make a U-turn at the junction.",
        "In 400 meters, at the roundabout, take the second exit.",
    )

    @Test fun everyWorldKeepsDistanceDirectionAndRoadNames() {
        for (world in World.entries) {
            val voice = PersonaVoice()
            samples.forEach { original ->
                val spoken = voice.rewrite(original, world)
                Regex("""\d+(\.\d+)?""").findAll(original).forEach { assertTrue("$world dropped ${it.value}: $spoken", spoken.contains(it.value)) }
                Regex("""onto ([A-Z][A-Za-z ]+?)[.,]""").find(original)?.let { assertTrue("$world lost the road name: $spoken", spoken.contains(it.groupValues[1])) }
                Regex("""\b(left|right)\b""", RegexOption.IGNORE_CASE).find(original)?.let {
                    assertTrue("$world lost the direction: $spoken", spoken.contains(it.value, ignoreCase = true))
                    val other = if (it.value.equals("left", true)) "right" else "left"
                    assertFalse("$world flipped the direction: $spoken", spoken.contains(other, ignoreCase = true))
                }
                assertTrue("$world produced empty speech", spoken.isNotBlank())
            }
        }
    }

    @Test fun worldsSoundDifferentFromEachOtherAndFromTheDefault() {
        val original = "In 200 meters, turn left onto Grand Trunk Road."
        val spoken = World.entries.associateWith { PersonaVoice().rewrite(original, it) }
        assertTrue(spoken.values.none { it == original })
        assertTrue("Expected at least 8 distinct phrasings, got ${spoken.values.toSet().size}", spoken.values.toSet().size >= 8)
    }

    @Test fun variesBetweenInstructionsInOneTrip() {
        val voice = PersonaVoice()
        val phrases = (1..6).map { voice.rewrite("Turn left onto Main Street.", World.OLD_WEST) }
        assertTrue(phrases.toSet().size >= 3)
    }

    @Test fun addressIsAddedOnlySometimes() {
        val voice = PersonaVoice()
        val addressed = (0 until 9).map { voice.rewrite("Turn right.", World.OLD_WEST) }
            .count { it.contains("partner") || it.contains("friend") || it.contains("traveller") }
        assertTrue("Expected a few but not all instructions addressed, got $addressed of 9", addressed in 1..5)
    }

    @Test fun narratorStylesEveryInstructionButKeepsTimingIdentityAndDetails() {
        for (world in World.entries) {
            val original = uniffi.ferrostar.SpokenInstruction("In 200 metres, turn left onto King's Road.", "<speak>x</speak>", 25.0, java.util.UUID.randomUUID())
            val spoken = WorldVoiceNarrator { 0 }.decorate(original, world)
            assertTrue("$world should restyle even an imminent turn: ${spoken.text}", spoken.text != original.text)
            assertTrue(spoken.text.contains("200 metres") && spoken.text.contains("King's Road") && spoken.text.contains("left", true))
            assertEquals(original.utteranceId, spoken.utteranceId)
            assertEquals(original.triggerDistanceBeforeManeuver, spoken.triggerDistanceBeforeManeuver, 0.0)
            // Imminent turns still carry no extra flavour sentence.
            assertFalse(spoken.text.contains(world.voiceStyle().opening))
        }
    }
}
