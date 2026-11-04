package com.waypoint.app

/** Serializes SDK mutation with trip transitions, including work queued on another dispatcher. */
internal class GuidanceSessionGate {
    private val lock = Any()
    private var generation = 0L

    fun <T> read(block: (Long) -> T): T = synchronized(lock) { block(generation) }

    fun transition(action: () -> Unit) = synchronized(lock) {
        generation++
        action()
    }

    fun applyIfCurrent(ticket: Long, action: () -> Unit): Boolean = synchronized(lock) {
        if(ticket != generation) false else {
            action()
            true
        }
    }
}
