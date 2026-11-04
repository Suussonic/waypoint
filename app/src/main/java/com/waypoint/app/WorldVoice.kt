package com.waypoint.app

import com.stadiamaps.ferrostar.core.SpokenInstructionObserver
import kotlinx.coroutines.flow.StateFlow
import uniffi.ferrostar.SpokenInstruction

/** Original brief phrases; route wording and the selected Android voice remain authoritative. */
internal data class WorldVoiceStyle(val opening: String,val cue: String,val arrival: String,
                                    val pitch: Float=1f,val pace: Float=1f)

internal fun World.voiceStyle(): WorldVoiceStyle=when(this) {
    World.LOS_SANTOS -> WorldVoiceStyle("The city is yours.","Next move.","You're at your spot.",.98f,1.02f)
    World.OLD_WEST -> WorldVoiceStyle("Let's ride, partner.","Follow the trail.","End of the trail, partner.",.94f,.97f)
    World.NIGHT_CITY -> WorldVoiceStyle("Route linked.","Next waypoint.","Connection complete.",1.02f,1.02f)
    World.PIXEL -> WorldVoiceStyle("A new adventure begins.","One block at a time.","You've found your destination.",1.04f,1.01f)
    World.GREEN_TERMINAL -> WorldVoiceStyle("Your route is ready, explorer.","Next waypoint ahead.","Exploration logged.",1f,.99f)
    World.SAN_ANDREAS -> WorldVoiceStyle("Back on the road.","Next move.","You're at your spot.",.98f,1.01f)
    World.HEAT -> WorldVoiceStyle("Your night route is ready.","Keep to the route.","Time to park up.",1f,1.02f)
    World.RIVIA -> WorldVoiceStyle("Your path is set, traveller.","Follow the path.","A moment to rest, traveller.",.95f,.97f)
    World.VICE_COAST -> WorldVoiceStyle("Your coastal route is ready.","On to the next turn.","Enjoy the stop.",1.02f,.99f)
    World.LIBERTY -> WorldVoiceStyle("Your city route is set.","Next turn ahead.","You've reached your spot.",.97f,1f)
    World.CTOS -> WorldVoiceStyle("City route connected.","Next connection.","Destination connected.",1.01f,1.02f)
    World.HYRULE -> WorldVoiceStyle("A new path awaits.","Onward, explorer.","Your journey brings you here.",1.02f,.98f)
    World.SKYRIM -> WorldVoiceStyle("The road awaits, traveller.","Follow the road.","Rest here, traveller.",.95f,.97f)
    World.LANDS_BETWEEN -> WorldVoiceStyle("A golden path awaits.","The path continues.","A place to pause.",.96f,.97f)
    World.HORIZON -> WorldVoiceStyle("The open road is calling.","Enjoy the journey.","Welcome to your next stop.",1.03f,1.02f)
    World.ASSASSIN -> WorldVoiceStyle("The route is synchronized.","Follow the waypoint.","Destination synchronized.",.98f,.99f)
    World.FAR_CRY -> WorldVoiceStyle("Your county route is ready.","Follow the road ahead.","You've reached your stop.",.97f,.99f)
    World.GHOST -> WorldVoiceStyle("Your path is charted.","Onward along the path.","Your journey pauses here.",.96f,.97f)
    World.BRIDGES -> WorldVoiceStyle("Delivery route plotted.","Next connection ahead.","Your destination is connected.",.98f,.98f)
    World.HORIZON_WILDS -> WorldVoiceStyle("A trail is waiting.","Follow the trail ahead.","Your next stop is here.",1.01f,.98f)
    World.SPIDER -> WorldVoiceStyle("Your neighbourhood route is ready.","Next move ahead.","Welcome to your stop.",1.03f,1.02f)
    World.HOGWARTS -> WorldVoiceStyle("Your journey is charted.","The next path awaits.","A little magic in the journey.",1.02f,.98f)
    World.STARFIELD -> WorldVoiceStyle("Surface route plotted.","Next waypoint confirmed.","Destination reached, explorer.",.99f,.99f)
    World.KINGDOM -> WorldVoiceStyle("The road is ready, traveller.","Along the next road.","Rest here a while.",.95f,.97f)
    World.SAINTS -> WorldVoiceStyle("Your city route is ready.","Your next move.","You've reached your spot.",1.02f,1.02f)
    World.JUST_CAUSE -> WorldVoiceStyle("Your island route is plotted.","Next waypoint ahead.","Welcome to your stop.",.99f,1.01f)
    World.MAFIA -> WorldVoiceStyle("Your route through town is set.","Next turn ahead.","This is your stop.",.94f,.98f)
    World.DIVISION -> WorldVoiceStyle("Agent, route plotted.","Waypoint confirmed.","Destination confirmed, agent.",.98f,1f)
    World.SEA_CHART -> WorldVoiceStyle("Course plotted, captain.","Follow the chart.","You've reached your port, captain.",.97f,.98f)
    World.DOS_ATLAS -> WorldVoiceStyle("Route loaded.","Next waypoint.","Route complete.",1f,.99f)
    World.AINCRAD -> WorldVoiceStyle("Your next adventure awaits.","On to the next waypoint.","A new place to explore.",1.03f,1f)
    World.COUNTER_STRIKE -> WorldVoiceStyle("Route confirmed.","Next waypoint confirmed.","Destination secured.",.98f,1.03f)
    World.DEFCON -> WorldVoiceStyle("Navigation command ready.","Course confirmed.","Destination confirmed.",.95f,.98f)
    World.FIELD_OPS -> WorldVoiceStyle("Route briefing complete.","Next waypoint.","Destination confirmed.",.96f,1.02f)
    World.RETRO_QUEST -> WorldVoiceStyle("A new journey begins.","Onward, adventurer.","You've reached a new stop.",1.04f,1f)
    World.HOME_CONSOLE -> WorldVoiceStyle("Let's go somewhere.","Next stop ahead.","Welcome to your destination.",1.03f,1f)
    World.NIER -> WorldVoiceStyle("Route analysis complete.","Pod confirms the next waypoint.","Destination reached. Navigator standing by.",.98f,.99f)
    World.STALKER -> WorldVoiceStyle("PDA route recorded.","Next waypoint, stalker.","Destination logged in your PDA.",.95f,.98f)
}

/** Adds personality after the useful instruction, with no extra words near an imminent turn. */
internal class WorldVoiceNarrator(private val now: ()->Long={System.nanoTime()/1_000_000}) {
    private var introduced=false
    private var lastFlavour:Long?=null
    // Bounded to three counters per world. Keep these across trips so restarting
    // guidance does not repeat the same greeting during an app session.
    private val variations=mutableMapOf<Pair<World,Int>,Int>()
    private val arrival=Regex("^(you have |you've )?(arrived|reached your destination)\\b",RegexOption.IGNORE_CASE)
    private val urgent=Regex("\\b(now|immediately|recalculating|rerouting)\\b",RegexOption.IGNORE_CASE)
    @Synchronized fun reset() { introduced=false;lastFlavour=null }
    @Synchronized fun decorate(instruction:SpokenInstruction,world:World?):SpokenInstruction {
        if(world==null || instruction.text.isBlank())return instruction
        val timestamp=now()
        val isArrival=arrival.containsMatchIn(instruction.text)
        if(!isArrival && (!instruction.triggerDistanceBeforeManeuver.isFinite() ||
            instruction.triggerDistanceBeforeManeuver<120.0 || urgent.containsMatchIn(instruction.text) ||
            instruction.text.length>180 || lastFlavour?.let { timestamp-it<45_000 }==true))return instruction
        val lines=world.voiceLines()
        val moment=if(isArrival)2 else if(!introduced)0 else 1
        val choices=when(moment) { 0 -> lines.openings;1 -> lines.cues;else -> lines.arrivals }
        val key=world to moment
        val index=variations[key] ?: 0
        val phrase=choices[index]
        variations[key]=(index+1)%choices.size
        introduced=true;lastFlavour=timestamp
        val separator=if(instruction.text.trimEnd().last() in ".!?")" " else ". "
        // Preserve distance, direction, road names, timing and utterance identity verbatim.
        return instruction.copy(text=instruction.text+separator+phrase,ssml=null)
    }
}

internal class WorldGuidanceSpeaker(
    private val delegate:SpokenInstructionObserver,
    private val world:()->World?,
    private val tone:(WorldVoiceStyle?)->Unit,
    private val narrator:WorldVoiceNarrator=WorldVoiceNarrator(),
):SpokenInstructionObserver {
    override val muteState:StateFlow<Boolean> get()=delegate.muteState
    override fun setMuted(isMuted:Boolean)=delegate.setMuted(isMuted)
    override fun stopAndClearQueue() { narrator.reset();delegate.stopAndClearQueue() }
    override fun onSpokenInstructionTrigger(spokenInstruction:SpokenInstruction) {
        val selected=world()
        tone(selected?.voiceStyle())
        delegate.onSpokenInstructionTrigger(narrator.decorate(spokenInstruction,selected))
    }
}
