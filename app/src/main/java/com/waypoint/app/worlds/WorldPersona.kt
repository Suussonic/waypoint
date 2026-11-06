package com.waypoint.app

/**
 * Spoken directions in each world's own manner. The wording is original and only rephrases the action; the
 * distance, direction, street names, exit numbers and timing in the instruction are kept exactly as given.
 * A {d} in a template stands for "left" or "right". Nothing here quotes a game or imitates an actor.
 */
internal data class Persona(
    val turns: List<String>,      // replaces "turn left/right"
    val slights: List<String>,    // replaces "bear / slight left/right"
    val straights: List<String>,  // replaces "continue straight / ahead"
    val uturns: List<String>,     // replaces "make a U-turn"
    val tags: List<String>,       // short address added to the end of some instructions
)

private val WESTERN = Persona(
    turns = listOf("swing {d}", "ease {d}", "wheel {d}"), slights = listOf("drift {d}", "lean {d}"),
    straights = listOf("keep riding straight", "hold the trail ahead"), uturns = listOf("wheel around and double back"),
    tags = listOf("partner", "friend", "traveller"))
private val FANTASY = Persona(
    turns = listOf("veer {d}", "sweep {d}", "bend {d}"), slights = listOf("curve {d}", "drift {d}"),
    straights = listOf("hold the road ahead", "keep to the road"), uturns = listOf("turn about"),
    tags = listOf("traveller", "wanderer", "adventurer"))
private val NEON = Persona(
    turns = listOf("cut {d}", "hook {d}", "peel {d}"), slights = listOf("nudge {d}", "drift {d}"),
    straights = listOf("hold the line", "stay on this line"), uturns = listOf("flip it around"),
    tags = listOf("runner", "driver"))
private val TERMINAL = Persona(
    turns = listOf("steer {d}", "route {d}", "go {d}"), slights = listOf("adjust {d}", "angle {d}"),
    straights = listOf("hold course", "maintain heading"), uturns = listOf("reverse heading"),
    tags = listOf("operator", "user"))
private val STREET = Persona(
    turns = listOf("hang a {d}", "take a {d}", "swing {d}"), slights = listOf("slide {d}", "ease {d}"),
    straights = listOf("keep it straight", "stay on this stretch"), uturns = listOf("whip it around"),
    tags = listOf("boss", "driver"))
private val RACING = Persona(
    turns = listOf("brake and turn {d}", "take the {d} line", "dive {d}"), slights = listOf("flick {d}", "ease {d}"),
    straights = listOf("hold the racing line", "keep it flat"), uturns = listOf("spin it around"),
    tags = listOf("driver", "racer"))
private val TACTICAL = Persona(
    turns = listOf("break {d}", "move {d}", "push {d}"), slights = listOf("angle {d}", "shift {d}"),
    straights = listOf("hold your heading", "stay on course"), uturns = listOf("reverse your heading"),
    tags = listOf("operator", "agent"))
private val EXPLORER = Persona(
    turns = listOf("head {d}", "strike out {d}", "follow the path {d}"), slights = listOf("bend {d}", "curve {d}"),
    straights = listOf("keep to the path", "press on straight"), uturns = listOf("double back"),
    tags = listOf("explorer", "scout"))
private val STEALTH = Persona(
    turns = listOf("slip {d}", "move {d} quietly", "turn {d} softly"), slights = listOf("drift {d}", "ease {d}"),
    straights = listOf("keep to the road", "stay the course"), uturns = listOf("turn back quietly"),
    tags = listOf("traveller", "wanderer"))
private val SEA = Persona(
    turns = listOf("put the helm {d}", "bring her {d}", "steer {d}"), slights = listOf("ease {d}", "angle {d}"),
    straights = listOf("hold your heading", "keep her steady"), uturns = listOf("come about"),
    tags = listOf("captain", "sailor"))
private val PIXEL = Persona(
    turns = listOf("build {d}", "hop {d}", "go {d}"), slights = listOf("nudge {d}", "step {d}"),
    straights = listOf("keep building straight", "stay on the path"), uturns = listOf("turn the cart around"),
    tags = listOf("crafter", "adventurer"))

internal fun World.persona(): Persona = when (this) {
    World.OLD_WEST -> WESTERN
    World.RIVIA, World.SKYRIM, World.LANDS_BETWEEN, World.KINGDOM, World.HOGWARTS, World.HYRULE,
    World.AINCRAD, World.RETRO_QUEST -> FANTASY
    World.NIGHT_CITY, World.CTOS, World.SPIDER -> NEON
    World.GREEN_TERMINAL, World.DOS_ATLAS, World.DEFCON, World.STARFIELD, World.NIER, World.HOME_CONSOLE -> TERMINAL
    World.LOS_SANTOS, World.SAN_ANDREAS, World.VICE_COAST, World.LIBERTY, World.SAINTS, World.MAFIA -> STREET
    World.HEAT, World.HORIZON -> RACING
    World.DIVISION, World.COUNTER_STRIKE, World.FIELD_OPS, World.FAR_CRY, World.STALKER -> TACTICAL
    World.HORIZON_WILDS, World.BRIDGES, World.JUST_CAUSE -> EXPLORER
    World.GHOST, World.ASSASSIN -> STEALTH
    World.SEA_CHART -> SEA
    World.PIXEL -> PIXEL
}

/** Rewrites the action in a spoken instruction and sometimes adds a short address, keeping every detail. */
internal class PersonaVoice {
    private var count = 0
    private val turn = Regex("""\bturn (left|right)\b""", RegexOption.IGNORE_CASE)
    private val slight = Regex("""\b(?:bear|slight|slightly) (left|right)\b""", RegexOption.IGNORE_CASE)
    private val straight = Regex("""\bcontinue (?:straight|ahead)\b""", RegexOption.IGNORE_CASE)
    private val uturn = Regex("""\bmake a u-?turn\b""", RegexOption.IGNORE_CASE)

    @Synchronized fun reset() { count = 0 }

    @Synchronized fun rewrite(text: String, world: World): String {
        val p = world.persona()
        val pick = count++
        fun choose(list: List<String>) = list[pick % list.size]
        fun direction(template: String, d: String) = template.replace("{d}", d.lowercase())
        var out = text
        var changed = false
        fun sub(regex: Regex, make: (MatchResult) -> String) {
            val match = if (changed) null else regex.find(out)
            if (match != null) { out = out.replaceRange(match.range, make(match)); changed = true }
        }
        sub(turn) { direction(choose(p.turns), it.groupValues[1]) }
        sub(slight) { direction(choose(p.slights), it.groupValues[1]) }
        sub(straight) { choose(p.straights) }
        sub(uturn) { choose(p.uturns) }
        // Capitalise if the rewritten action starts the sentence.
        out = out.replaceFirstChar { it.uppercase() }
        // Address the listener on about every third instruction, so it adds character without nagging.
        if (pick % 3 == 1 && out.isNotBlank()) {
            val tag = choose(p.tags)
            out = out.trimEnd().let { s -> if (s.last() in ".!?") s.dropLast(1) + ", $tag" + s.last() else "$s, $tag" }
        }
        return out
    }
}
