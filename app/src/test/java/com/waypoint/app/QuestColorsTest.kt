package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class QuestColorsTest {
    @Test fun taskTextAndActionsRemainReadableInEveryWorld() {
        World.entries.forEach { world ->
            val palette = hudPalette(world)
            val surface = palette.panel.copy(alpha = 1f)
            listOf(palette.foreground, palette.muted, palette.accent).forEach { ink ->
                assertTrue("${world.name} readable task ink", questContrast(questInk(ink, surface), surface) >= 4.5f)
            }
            val primary = questInk(palette.accent, surface)
            assertTrue("${world.name} button label", questContrast(questInk(surface, primary), primary) >= 4.5f)
        }
    }
}
