package com.waypoint.app

import com.stadiamaps.ferrostar.core.SpokenInstructionObserver
import kotlinx.coroutines.flow.StateFlow
import uniffi.ferrostar.SpokenInstruction

/** Original brief phrases; route wording and the selected Android voice remain authoritative. */
internal data class WorldVoiceStyle(val opening: String,val cue: String,val arrival: String,
                                    val pitch: Float,val pace: Float,
                                    val voiceLocale: String,val voiceVariant: Int=0)

internal fun World.voiceStyle(): WorldVoiceStyle=when(this) {
    World.LOS_SANTOS -> WorldVoiceStyle("City grid unlocked. Your route is set.","Next turn; keep the boulevard in view.","Arrived. Park up and take the city in.",.99f,1.04f,"en-US",0)
    World.OLD_WEST -> WorldVoiceStyle("The trail is marked. Ride easy, partner.","Keep to the trail; the next bend is ahead.","Trail's end. Rest easy, partner.",.92f,.95f,"en-US",1)
    World.NIGHT_CITY -> WorldVoiceStyle("Link established. Route is live.","Street grid update. Next junction incoming.","Connection complete. Destination logged.",1.08f,1.05f,"en-US",2)
    World.PIXEL -> WorldVoiceStyle("Map crafted. Your path is set.","One block ahead; follow the marker.","Waypoint reached. Quest checkpoint clear.",1.14f,1.00f,"en-AU",0)
    World.GREEN_TERMINAL -> WorldVoiceStyle("Terminal online. Route file loaded.","Next turn; keep the signal in sight.","Location recorded. Terminal standing by.",.96f,.94f,"en-US",3)
    World.SAN_ANDREAS -> WorldVoiceStyle("Road atlas open. Let's roll.","Next street up; stay on the route.","You're at the spot. Route complete.",1.00f,1.02f,"en-US",1)
    World.HEAT -> WorldVoiceStyle("Night run mapped. Stay smooth.","Racing line bends ahead. Settle in.","Run complete. Cool down at your stop.",.95f,1.07f,"en-GB",0)
    World.RIVIA -> WorldVoiceStyle("Path entered in the travel ledger, traveller.","Stay with the trail; your next turn is near.","The journey rests here, traveller.",.94f,.96f,"en-GB",1)
    World.VICE_COAST -> WorldVoiceStyle("Coastal route plotted. The boulevard is yours.","Waterfront turn ahead; keep cruising.","You're at the stop. Enjoy the view.",1.06f,.98f,"en-US",2)
    World.LIBERTY -> WorldVoiceStyle("City grid set. Keep the route close.","Junction ahead; follow the marked street.","Address reached. You're clear.",.98f,.99f,"en-US",0)
    World.CTOS -> WorldVoiceStyle("Network synced. Route channel is open.","Street link ahead. Follow the signal.","Destination synced. Session complete.",1.04f,.96f,"en-US",1)
    World.HYRULE -> WorldVoiceStyle("Slate charged. Your path is recorded.","Next turn marked on the slate.","The slate confirms your waypoint.",1.08f,.99f,"en-GB",2)
    World.SKYRIM -> WorldVoiceStyle("Northern road plotted. Keep your compass close.","The road bends ahead; follow the marker.","Journey's end. Rest at the next hold.",.93f,.94f,"en-GB",0)
    World.LANDS_BETWEEN -> WorldVoiceStyle("Golden route traced. Keep your bearings.","Follow the light to the next turn.","Waymark reached. Pause here.",.90f,.92f,"en-GB",1)
    World.HORIZON -> WorldVoiceStyle("Open road mapped. Your next stop is clear.","Keep the pace; the bend is close.","Route complete. Take in the view.",1.12f,1.04f,"en-AU",1)
    World.ASSASSIN -> WorldVoiceStyle("Memory sequence synchronized.","Waypoint updating. Follow the next marker.","Sequence complete. Location archived.",.99f,.93f,"en-GB",2)
    World.FAR_CRY -> WorldVoiceStyle("County map open. The road is yours.","Next turn ahead. Watch the tree line.","You made it. Stay alert out there.",.97f,.95f,"en-US",2)
    World.GHOST -> WorldVoiceStyle("Island route inked. Travel with care.","Next turn along the road. Keep your pace.","Your journey pauses here.",.95f,.91f,"en-US",1)
    World.BRIDGES -> WorldVoiceStyle("Delivery path loaded. Link established.","Next connection ahead. Stay on the route.","Delivery point reached. Route complete.",1.02f,.97f,"en-US",0)
    World.HORIZON_WILDS -> WorldVoiceStyle("Field path mapped. The trail is open.","Marker ahead; keep to the path.","Waypoint reached. Field notes updated.",1.10f,1.01f,"en-US",2)
    World.SPIDER -> WorldVoiceStyle("Borough grid mapped. The next block is marked.","Turn ahead; keep the skyline in view.","Address reached. The city is yours.",1.06f,1.07f,"en-US",1)
    World.HOGWARTS -> WorldVoiceStyle("Atlas prepared. Your journey is charted.","Crossroads ahead; follow the marked path.","Destination reached. Your journey rests here.",1.10f,.96f,"en-GB",3)
    World.STARFIELD -> WorldVoiceStyle("Surface coordinates plotted. Route is live.","Course correction ahead; follow the marker.","Coordinates reached. Survey complete.",1.01f,.94f,"en-US",3)
    World.KINGDOM -> WorldVoiceStyle("Roads recorded. Keep a measured pace.","Next crossing ahead; follow the road.","Wayfarer's stop reached. Rest awhile.",.91f,.95f,"en-GB",4)
    World.SAINTS -> WorldVoiceStyle("City run is queued. Route is live.","Next turn ahead. Keep the wheels moving.","You're at the spot. Run complete.",1.08f,1.09f,"en-US",3)
    World.JUST_CAUSE -> WorldVoiceStyle("Island route marked. Keep your bearings.","Coast road ahead; follow the marker.","Route complete. Take in the island view.",1.03f,1.06f,"en-AU",2)
    World.MAFIA -> WorldVoiceStyle("Itinerary is set. The streets are mapped.","Next corner ahead; stay with the route.","Address reached. You're all set.",.89f,.94f,"en-US",4)
    World.DIVISION -> WorldVoiceStyle("Agent, route package loaded.","Checkpoint ahead. Follow the route data.","Destination confirmed, agent.",.97f,.98f,"en-US",4)
    World.SEA_CHART -> WorldVoiceStyle("Course laid in. The chart is ready, captain.","Next bearing ahead; keep to the course.","Port reached. Make fast, captain.",.96f,.95f,"en-GB",2)
    World.DOS_ATLAS -> WorldVoiceStyle("System map loaded. Navigation routine ready.","Next sector; keep the route pointer in view.","Destination recorded. Process complete.",1.02f,.94f,"en-GB",1)
    World.AINCRAD -> WorldVoiceStyle("Adventure path set. Your waypoint is queued.","Next waypoint ahead; continue along the path.","Destination logged. The journey is complete.",1.12f,1.00f,"en-US",5)
    World.COUNTER_STRIKE -> WorldVoiceStyle("Route confirmed. Sector data is ready.","Next turn in sight; hold your course.","Destination secured. Route complete.",.98f,1.08f,"en-US",5)
    World.DEFCON -> WorldVoiceStyle("Navigation plot initialized. Vector line held.","Course change ahead. Confirm the next vector.","Destination entered in the command log.",.91f,.98f,"en-GB",5)
    World.FIELD_OPS -> WorldVoiceStyle("Briefing complete. Route is marked.","Next turn ahead; maintain course.","Objective location reached. Stand by.",.94f,1.04f,"en-US",6)
    World.RETRO_QUEST -> WorldVoiceStyle("Quest map loaded. Your next mark is set.","Next turn ahead; carry on, adventurer.","Checkpoint reached. Add it to your journal.",1.14f,.98f,"en-AU",2)
    World.HOME_CONSOLE -> WorldVoiceStyle("Navigation ready. Let's go somewhere.","Next stop ahead. Follow the map.","You have arrived. Take a moment.",1.11f,.97f,"en-AU",3)
    World.NIER -> WorldVoiceStyle("Pod navigation initialized. Route analysis complete.","Next waypoint confirmed. Data link stable.","Destination reached. Navigator standing by.",1.00f,.95f,"en-GB",7)
    World.STALKER -> WorldVoiceStyle("PDA route recorded. Stay on the marked track.","Next waypoint, stalker. Keep your scanner close.","Destination logged in your PDA.",.93f,1.00f,"en-GB",6)
}

/** Adds personality after the useful instruction, with no extra words near an imminent turn. */
internal class WorldVoiceNarrator(private val personaStyling: Boolean=true,private val now: ()->Long={System.nanoTime()/1_000_000}) {
    private var introduced=false
    private var lastFlavour:Long?=null
    // Bounded to three counters per world. Keep these across trips so restarting
    // guidance does not repeat the same greeting during an app session.
    private val variations=mutableMapOf<Pair<World,Int>,Int>()
    private val arrival=Regex("^(you have |you've )?(arrived|reached your destination)\\b",RegexOption.IGNORE_CASE)
    private val urgent=Regex("\\b(now|immediately|recalculating|rerouting)\\b",RegexOption.IGNORE_CASE)
    private val persona=PersonaVoice()
    @Synchronized fun reset() { introduced=false;lastFlavour=null;persona.reset() }
    @Synchronized fun decorate(instruction:SpokenInstruction,world:World?):SpokenInstruction {
        if(world==null || instruction.text.isBlank())return instruction
        // Every instruction is voiced in the world's manner; the existing flavour line below is added on top
        // for calm moments. Distance, direction and street names are never altered.
        val styled=if(personaStyling)persona.rewrite(instruction.text,world) else instruction.text
        val instruction=if(styled==instruction.text)instruction else instruction.copy(text=styled,ssml=null)
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
    private val fxVoice:FxVoice?=null,
):SpokenInstructionObserver {
    override val muteState:StateFlow<Boolean> get()=delegate.muteState
    override fun setMuted(isMuted:Boolean) {
        delegate.setMuted(isMuted)
        if(isMuted)fxVoice?.stop()
    }
    override fun stopAndClearQueue() { narrator.reset();fxVoice?.stop();delegate.stopAndClearQueue() }
    override fun onSpokenInstructionTrigger(spokenInstruction:SpokenInstruction) {
        speakStyled(spokenInstruction,world())
    }
    /** Worded in the world's manner and run through its effects; the plain Android voice is the fallback. */
    private fun speakStyled(spokenInstruction:SpokenInstruction,selected:World?) {
        val style=selected?.voiceStyle()
        val decorated=narrator.decorate(spokenInstruction,selected)
        if(fxVoice!=null && selected!=null && !muteState.value) {
            fxVoice.speak(decorated.text,selected.voiceFx(),style) {
                tone(style)
                delegate.onSpokenInstructionTrigger(decorated)
            }
            return
        }
        tone(style)
        delegate.onSpokenInstructionTrigger(decorated)
    }
}
