package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class QuestColorsTest {
    @Test fun taskTextAndActionsRemainReadableInEveryWorld() {
        World.entries.forEach { world ->
            listOf(false,true).forEach { night ->
                val palette = hudPalette(world,night)
                val surface = palette.panel.copy(alpha = 1f)
                val accentContrast=questContrast(palette.accent,surface)
                assertTrue("${world.name} night=$night theme accent contrast=$accentContrast",accentContrast>=4.5f)
                assertTrue("${world.name} night=$night muted labels",questContrast(palette.muted,surface)>=4.5f)
                listOf(palette.foreground, palette.muted, palette.accent).forEach { ink ->
                    assertTrue("${world.name} night=$night readable task ink", questContrast(questInk(ink, surface), surface) >= 4.5f)
                }
                val primary = questInk(palette.accent, surface)
                assertTrue("${world.name} night=$night button label", questContrast(questInk(surface, primary), primary) >= 4.5f)
            }
        }
    }
}
