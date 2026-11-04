package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class CarMenuGroupsTest {
    @Test fun everyDestinationRemainsReachableWithinTheHostLimit() {
        for(limit in 2..12) for(size in listOf(0,1,6,12,36,50,100,1000)) {
            val items = (0 until size).toList()
            fun visit(group: List<Int>): List<Int> {
                val children = carMenuGroups(group, limit)
                assertTrue(children.size <= limit)
                return children.flatMap { if(it.size > 1)visit(it) else it }
            }
            assertEquals(items, visit(items))
        }
    }
    @Test fun worldsAppearExactlyOnceAndFitTheMinimumHostBudget() {
        assertTrue(carWorldGroups.size <= 6)
        assertTrue(carWorldGroups.values.all { carMenuGroups(it,6).size <= 6 })
        val worlds = carWorldGroups.values.flatten()
        assertEquals(World.entries.toSet(), worlds.toSet())
        assertEquals(worlds.size, worlds.distinct().size)
    }
}
