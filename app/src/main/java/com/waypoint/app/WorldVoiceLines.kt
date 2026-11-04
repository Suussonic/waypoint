package com.waypoint.app

/** Authored game-inspired lines, not quotations or recordings from the games. */
internal data class WorldVoiceLines(val openings:List<String>,val cues:List<String>,val arrivals:List<String>)

private fun WorldVoiceStyle.withLines(opening2:String,opening3:String,cue2:String,cue3:String,
                                     arrival2:String,arrival3:String)=WorldVoiceLines(
    listOf(opening,opening2,opening3),listOf(cue,cue2,cue3),listOf(arrival,arrival2,arrival3))

internal fun World.voiceLines():WorldVoiceLines=voiceStyle().let { style ->
    when(this) {
        World.LOS_SANTOS -> style.withLines(
            "City map loaded. Let's roll.","A new trip across town.",
            "Another piece of the city.","Your waypoint is on the map.",
            "That's your stop in the city.","Another city journey logged.")
        World.OLD_WEST -> style.withLines(
            "The trail is marked, partner.","Saddle up. Your route is ready.",
            "Another stretch of the trail.","Easy along the trail, partner.",
            "You've found your stopping place.","Time to rest the saddle, partner.")
        World.NIGHT_CITY -> style.withLines(
            "Neon grid connected.","City link online. Route ready.",
            "Navigation link holding.","Another waypoint on the grid.",
            "Waypoint reached. Link standing by.","Your stop is logged on the grid.")
        World.PIXEL -> style.withLines(
            "Map crafted. Adventure ready.","Your next waypoint is placed.",
            "A little more of the map explored.","Another stretch of your blocky adventure.",
            "Your adventure has a new stopping point.","This waypoint belongs on your map.")
        World.GREEN_TERMINAL -> style.withLines(
            "Personal navigator online.","Explorer, your map is loaded.",
            "Your navigator is keeping track.","Another entry for the travel log.",
            "Destination entered in the travel log.","Navigator standing by, explorer.")
        World.SAN_ANDREAS -> style.withLines(
            "Your neighbourhood route is ready.","A fresh trip through the city.",
            "Your next stop is on the map.","Another part of town ahead.",
            "That's the place you picked.","Another trip through town, logged.")
        World.HEAT -> style.withLines(
            "Street map loaded. Journey ready.","Your next stop is on the city map.",
            "Your navigator has the next waypoint.","The city map stays with you.",
            "This is your planned stop.","Journey logged. Time for a break.")
        World.RIVIA -> style.withLines(
            "Your travel map is unfolded.","A new journey is marked, traveller.",
            "Another mile for your journal.","The map holds your next waypoint.",
            "Your travel journal has a new entry.","The path has brought you here.")
        World.VICE_COAST -> style.withLines(
            "Your sunlit city map is loaded.","A fresh journey on the coast map.",
            "Your next stop is marked.","Another chapter in your city journey.",
            "Time to enjoy your next stop.","Your coastal navigator is standing by.")
        World.LIBERTY -> style.withLines(
            "Your city atlas is open.","Another city journey is planned.",
            "The next waypoint is marked.","Your city atlas is keeping pace.",
            "Your stop is here in the city.","This journey is in the city log.")
        World.CTOS -> style.withLines(
            "Navigation network ready.","City map connection established.",
            "Waypoint data is ready.","Your route link is active.",
            "Waypoint connection logged.","City navigator standing by.")
        World.HYRULE -> style.withLines(
            "Your slate has a new waypoint.","The travel map is ready, explorer.",
            "Another part of your adventure.","The slate is following your journey.",
            "Your slate has recorded this place.","A new place in your travel journal.")
        World.SKYRIM -> style.withLines(
            "Your atlas is open, traveller.","Another journey begins on the map.",
            "A little farther in your journey.","Your travel journal follows the road.",
            "Your journey has found its stopping place.","Mark this place in your journal.")
        World.LANDS_BETWEEN -> style.withLines(
            "Your next waypoint is marked in gold.","Your traveller's atlas is open.",
            "Another mark on your golden map.","The atlas keeps your destination in view.",
            "Your golden waypoint is reached.","The atlas records this stopping place.")
        World.HORIZON -> style.withLines(
            "Your road trip is mapped.","Another journey for your festival map.",
            "A new stretch of your road trip.","Your next waypoint is on the festival map.",
            "Another stop for your road-trip album.","Your road-trip navigator is standing by.")
        World.ASSASSIN -> style.withLines(
            "Your travel atlas is synchronized.","A new journey has been charted.",
            "Waypoint synchronization holds.","Another entry in your travel record.",
            "This location is in your travel record.","Your atlas records this destination.")
        World.FAR_CRY -> style.withLines(
            "County map unfolded.","Your next journey is marked on the county map.",
            "Another stretch of the county route.","Your county navigator is keeping track.",
            "Your stop is marked in the county log.","County navigator standing by.")
        World.GHOST -> style.withLines(
            "Your journey is drawn on the map.","The traveller's map is ready.",
            "Your journey continues on the map.","Another moment in the travel journal.",
            "Mark this place in your travel journal.","A quiet pause in your journey.")
        World.BRIDGES -> style.withLines(
            "Your next connection is on the map.","Journey plotted. Navigator connected.",
            "One more waypoint in your journey.","The route link stays with you.",
            "A new connection in your travel log.","Your navigator is ready for a pause.")
        World.HORIZON_WILDS -> style.withLines(
            "Your trail map is ready, explorer.","A new waypoint in the wilds atlas.",
            "Your travel map is tracking the journey.","Another waypoint for your field journal.",
            "Your field journal records this place.","A new stopping point in your travels.")
        World.SPIDER -> style.withLines(
            "Your friendly neighbourhood map is ready.","Another journey across the city grid.",
            "The neighbourhood map is keeping up.","Your next waypoint is in view on the map.",
            "Another neighbourhood stop logged.","Your city map is ready for a break.")
        World.HOGWARTS -> style.withLines(
            "Your enchanted atlas is open.","A new journey in your travel book.",
            "Another page in the enchanted atlas.","Your waypoint is marked in the travel book.",
            "Your atlas has a new destination entry.","A little wonder at your journey's end.")
        World.STARFIELD -> style.withLines(
            "Surface navigator online, explorer.","A new ground route is charted.",
            "Surface waypoint logged.","Your ground navigation link is active.",
            "Surface journey entered in the log.","Ground navigator standing by, explorer.")
        World.KINGDOM -> style.withLines(
            "Your traveller's map is unfolded.","A new road journey is charted.",
            "Another stretch for the travel journal.","Your atlas has the next waypoint.",
            "Your travel journal marks this stop.","The road has brought you to your stop.")
        World.SAINTS -> style.withLines(
            "City map ready for your next trip.","Your waypoint is marked across town.",
            "Your city navigator is on the job.","Another stop on your city map.",
            "Your city journey is logged.","That's your chosen stopping point.")
        World.JUST_CAUSE -> style.withLines(
            "Your field map is loaded.","A new route on the island atlas.",
            "Your field navigator is tracking the route.","Next waypoint logged in the atlas.",
            "Your field map records the stop.","Island navigator standing by.")
        World.MAFIA -> style.withLines(
            "Your town atlas is open.","The next stop is pencilled into the map.",
            "Another street on your town journey.","Your next stop stays on the map.",
            "That's the address on your itinerary.","Town journey logged. Enjoy the stop.")
        World.DIVISION -> style.withLines(
            "Agent, your navigation link is ready.","Field route added to your map.",
            "Navigation link active, agent.","Your field map has the next waypoint.",
            "Waypoint logged in the field journal.","Field navigator standing by, agent.")
        World.SEA_CHART -> style.withLines(
            "Your chart is open, captain.","A new journey on your captain's chart.",
            "Another mark on the chart, captain.","Your navigator is keeping the chart.",
            "Mark this stop in the captain's log.","Navigator standing by, captain.")
        World.DOS_ATLAS -> style.withLines(
            "Navigation program ready.","Journey loaded into the terminal.",
            "Waypoint record updated.","Navigation process active.",
            "Destination written to the travel log.","Navigation terminal standing by.")
        World.AINCRAD -> style.withLines(
            "Your journey window is ready.","A new waypoint in your adventure log.",
            "The adventure map is following along.","Another waypoint in your travel journal.",
            "Your adventure log records this place.","A new stopping point in your journey.")
        World.COUNTER_STRIKE -> style.withLines(
            "Navigation briefing ready.","Your radar waypoint is marked.",
            "Navigation channel active.","Your radar map has the next waypoint.",
            "Waypoint logged on the radar map.","Navigation channel standing by.")
        World.DEFCON -> style.withLines(
            "Navigation plot initialized.","Your route is on the command map.",
            "Waypoint plotted on the vector map.","Navigation plot remains active.",
            "Destination entered in the command log.","Vector navigator standing by.")
        World.FIELD_OPS -> style.withLines(
            "Field navigation ready.","Your route is marked on the briefing map.",
            "Navigation channel holding.","Next waypoint marked in the briefing.",
            "Destination logged in your field notes.","Field navigation standing by.")
        World.RETRO_QUEST -> style.withLines(
            "Your adventure atlas is ready.","A new path on your travel map.",
            "Another waypoint in your adventure.","Your atlas is keeping the travel log.",
            "This stop joins your adventure map.","A new place for your travel journal.")
        World.HOME_CONSOLE -> style.withLines(
            "Your next outing is ready.","A new journey is on your map.",
            "Your navigator is following along.","Another waypoint on your outing.",
            "Your outing has reached its next stop.","Your map is ready when you are.")
        World.NIER -> style.withLines(
            "Pod navigation initialized.","Your next journey is ready for analysis.",
            "Waypoint data confirmed.","Navigation support remains active.",
            "Journey record complete.","Pod navigation standing by at your destination.")
        World.STALKER -> style.withLines(
            "PDA navigator online, stalker.","Your route is marked in the PDA.",
            "PDA navigation link active.","Your field notes have the next waypoint.",
            "This stop is saved in the PDA log.","PDA navigator standing by, stalker.")
    }
}
