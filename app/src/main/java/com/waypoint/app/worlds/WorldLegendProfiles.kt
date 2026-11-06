package com.waypoint.app

/** Per-world naming for the live OSM place key; aliases keep their real place class beside them. */
internal data class WorldLegendProfile(
    val title: String,
    val aliases: Map<String, String>,
    val groupOrder: List<String> = emptyList(),
    val layout: WorldLegendLayout = WorldLegendLayout.GRID,
    val groupTitles: Map<String, String> = emptyMap(),
    val navigation: MapLegendNavigationLabels = MapLegendNavigationLabels("ROUTE", "POSITION", "DESTINATION"),
)

internal data class MapLegendNavigationLabels(val route: String, val position: String, val destination: String)

internal enum class WorldLegendLayout { GRID, LEDGER, RADAR, TABLE }

internal object WorldLegendProfiles {
    private val profiles = mapOf(
        World.LOS_SANTOS to WorldLegendProfile("STREET DIRECTORY", mapOf("restaurant" to "Grill / eatery", "pub" to "Night spot", "lodging" to "Motel", "fuel" to "Gas stop", "police" to "Police station", "train_station" to "Rail stop")),
        World.OLD_WEST to WorldLegendProfile("TRAIL LEDGER", mapOf("restaurant" to "Chow house", "pub" to "Saloon", "lodging" to "Wayfarer inn", "fuel" to "Supply stop", "police" to "Sheriff", "train_station" to "Rail depot")),
        World.NIGHT_CITY to WorldLegendProfile("CITY NETWORK", mapOf("restaurant" to "Food vendor", "pub" to "Nightlife", "lodging" to "Safehouse", "fuel" to "Charge stop", "police" to "Security HQ", "train_station" to "Metro stop")),
        World.PIXEL to WorldLegendProfile("WORLD INDEX", mapOf("restaurant" to "Food shop", "pub" to "Tavern", "lodging" to "Rest point", "fuel" to "Fuel point", "police" to "Guard post", "train_station" to "Transit stop")),
        World.GREEN_TERMINAL to WorldLegendProfile("SIGNAL DIRECTORY", mapOf("restaurant" to "Supply node", "pub" to "Social node", "lodging" to "Safe node", "fuel" to "Power stop", "police" to "Control post", "train_station" to "Transit node")),
        World.SAN_ANDREAS to WorldLegendProfile("COUNTY DIRECTORY", mapOf("restaurant" to "Drive-in", "pub" to "Bar", "lodging" to "Motel", "fuel" to "Gas station", "police" to "Sheriff", "train_station" to "Rail yard")),
        World.HEAT to WorldLegendProfile("NIGHT DISPATCH", mapOf("restaurant" to "Late-night diner", "pub" to "Night bar", "lodging" to "Hotel", "fuel" to "Fuel stop", "police" to "Station", "train_station" to "Rail stop")),
        World.RIVIA to WorldLegendProfile("TRAVELER’S LEDGER", mapOf("restaurant" to "Inn kitchen", "pub" to "Alehouse", "lodging" to "Inn", "fuel" to "Supply stop", "police" to "Guard post", "train_station" to "Travel hub")),
        World.VICE_COAST to WorldLegendProfile("COASTAL DIRECTORY", mapOf("restaurant" to "Seafood / diner", "pub" to "Nightclub", "lodging" to "Beach hotel", "fuel" to "Fuel stop", "police" to "Police station", "train_station" to "Transit stop")),
        World.LIBERTY to WorldLegendProfile("CITY GRID", mapOf("restaurant" to "Deli / diner", "pub" to "Bar", "lodging" to "Hotel", "fuel" to "Garage / fuel", "police" to "Precinct", "train_station" to "Subway / rail")),
        World.CTOS to WorldLegendProfile("CONNECTED DIRECTORY", mapOf("restaurant" to "Food node", "pub" to "Social node", "lodging" to "Stay node", "fuel" to "Charge node", "police" to "Security node", "train_station" to "Transit node")),
        World.HYRULE to WorldLegendProfile("SHEIKAH INDEX", mapOf("restaurant" to "Rations", "pub" to "Inn", "lodging" to "Stable / inn", "fuel" to "Supply stop", "police" to "Guard post", "train_station" to "Travel hub")),
        World.SKYRIM to WorldLegendProfile("HOLD GUIDE", mapOf("restaurant" to "Cookhouse", "pub" to "Mead hall", "lodging" to "Inn", "fuel" to "Supply stop", "police" to "Hold guard", "train_station" to "Carriage stop")),
        World.LANDS_BETWEEN to WorldLegendProfile("LANDMARK LEDGER", mapOf("restaurant" to "Provisions", "pub" to "Resting place", "lodging" to "Waystation", "fuel" to "Supply cache", "police" to "Guard post", "train_station" to "Travel hub")),
        World.HORIZON to WorldLegendProfile("RALLY DIRECTORY", mapOf("restaurant" to "Pit stop", "pub" to "Meet-up", "lodging" to "Guest lodge", "fuel" to "Refuel stop", "police" to "Race control", "train_station" to "Transit stop")),
        World.ASSASSIN to WorldLegendProfile("REGION REGISTER", mapOf("restaurant" to "Market kitchen", "pub" to "Coffeehouse", "lodging" to "Caravanserai", "fuel" to "Supply post", "police" to "Watch post", "train_station" to "Rail stop")),
        World.FAR_CRY to WorldLegendProfile("COUNTY FIELD GUIDE", mapOf("restaurant" to "Local diner", "pub" to "Bar", "lodging" to "Lodge", "fuel" to "Fuel stop", "police" to "Sheriff station", "train_station" to "Transit stop")),
        World.GHOST to WorldLegendProfile("TSUSHIMA TRAVELER’S MAP", mapOf("restaurant" to "Tea house", "pub" to "Inn", "lodging" to "Ryokan", "fuel" to "Supply stop", "police" to "Guard post", "train_station" to "Travel link")),
        World.BRIDGES to WorldLegendProfile("FIELD TABLET", mapOf("restaurant" to "Canteen", "pub" to "Rest stop", "lodging" to "Private room", "fuel" to "Power charger", "police" to "Porter station", "train_station" to "Transit center")),
        World.HORIZON_WILDS to WorldLegendProfile("HUNTER’S ATLAS", mapOf("restaurant" to "Food source", "pub" to "Settlement", "lodging" to "Camp lodge", "fuel" to "Supply station", "police" to "Outpost", "train_station" to "Transit point")),
        World.SPIDER to WorldLegendProfile("BOROUGH DIRECTORY", mapOf("restaurant" to "Corner eatery", "pub" to "Night spot", "lodging" to "Hotel", "fuel" to "Fuel stop", "police" to "Precinct", "train_station" to "Subway stop")),
        World.HOGWARTS to WorldLegendProfile("HIGHLANDS FIELD GUIDE", mapOf("restaurant" to "Tea room", "pub" to "Inn", "lodging" to "Guest house", "fuel" to "Service stop", "police" to "Constabulary", "train_station" to "Rail station")),
        World.STARFIELD to WorldLegendProfile("SURFACE SURVEY", mapOf("restaurant" to "Food kiosk", "pub" to "Hab lounge", "lodging" to "Habitat", "fuel" to "Refuel point", "police" to "Security post", "train_station" to "Transit port")),
        World.KINGDOM to WorldLegendProfile("BOHEMIA REGISTER", mapOf("restaurant" to "Cookhouse", "pub" to "Alehouse", "lodging" to "Inn", "fuel" to "Supply stop", "police" to "Town watch", "train_station" to "Rail station")),
        World.SAINTS to WorldLegendProfile("STEELPORT DIRECTORY", mapOf("restaurant" to "Food truck", "pub" to "Nightclub", "lodging" to "Safehouse", "fuel" to "Fuel stop", "police" to "Police HQ", "train_station" to "Transit stop")),
        World.JUST_CAUSE to WorldLegendProfile("MEDICI FIELD GUIDE", mapOf("restaurant" to "Food stall", "pub" to "Beach bar", "lodging" to "Guest house", "fuel" to "Refuel stop", "police" to "Security post", "train_station" to "Rail stop")),
        World.MAFIA to WorldLegendProfile("LOST HEAVEN DIRECTORY", mapOf("restaurant" to "Trattoria", "pub" to "Speakeasy", "lodging" to "Hotel", "fuel" to "Garage", "police" to "Precinct", "train_station" to "Rail station")),
        World.DIVISION to WorldLegendProfile("FIELD INTELLIGENCE", mapOf("restaurant" to "Supply point", "pub" to "Safehouse", "lodging" to "Shelter", "fuel" to "Refuel point", "police" to "Safe area", "train_station" to "Transit hub")),
        World.SEA_CHART to WorldLegendProfile("CHART KEY", mapOf("restaurant" to "Galley", "pub" to "Harbor tavern", "lodging" to "Harbor inn", "fuel" to "Fuel dock", "police" to "Harbor patrol", "train_station" to "Rail terminal")),
        World.DOS_ATLAS to WorldLegendProfile("ATLAS DIRECTORY", mapOf("restaurant" to "Food vendor", "pub" to "Social venue", "lodging" to "Waystation", "fuel" to "Fuel depot", "police" to "Station", "train_station" to "Transit hub")),
        World.AINCRAD to WorldLegendProfile("FIELD INDEX", mapOf("restaurant" to "Cookhouse", "pub" to "Guild hall", "lodging" to "Inn", "fuel" to "Supply stop", "police" to "Guard post", "train_station" to "Transit gate")),
        World.COUNTER_STRIKE to WorldLegendProfile("RADAR KEY", mapOf("restaurant" to "Supply", "pub" to "Team stop", "lodging" to "Staging area", "fuel" to "Refuel", "police" to "Security", "train_station" to "Transit")),
        World.DEFCON to WorldLegendProfile("VECTOR DISPLAY", mapOf("restaurant" to "Supply point", "pub" to "Social site", "lodging" to "Shelter", "fuel" to "Fuel point", "police" to "Civic station", "train_station" to "Transit link")),
        World.FIELD_OPS to WorldLegendProfile("OPERATIONS MAP", mapOf("restaurant" to "Supply stop", "pub" to "Rest point", "lodging" to "Shelter", "fuel" to "Refuel", "police" to "Safety post", "train_station" to "Transit point")),
        World.RETRO_QUEST to WorldLegendProfile("QUEST ATLAS", mapOf("restaurant" to "Inn kitchen", "pub" to "Tavern", "lodging" to "Inn", "fuel" to "Supply point", "police" to "Guard post", "train_station" to "Travel node")),
        World.HOME_CONSOLE to WorldLegendProfile("PLAYGROUND MAP", mapOf("restaurant" to "Food stop", "pub" to "Social spot", "lodging" to "Stay point", "fuel" to "Charge spot", "police" to "Safety center", "train_station" to "Transit stop")),
        World.STALKER to WorldLegendProfile("PDA FIELD DATA", mapOf("restaurant" to "Canteen", "pub" to "Camp hub", "lodging" to "Shelter", "fuel" to "Fuel point", "police" to "Checkpoint", "train_station" to "Rail stop")),
        World.NIER to WorldLegendProfile("MACHINE ATLAS", mapOf("restaurant" to "Supply counter", "pub" to "Camp hub", "lodging" to "Rest point", "fuel" to "Supply station", "police" to "Security node", "train_station" to "Transit hub")),
    )

    private val groupOrders = mapOf(
        World.LOS_SANTOS to listOf("essential", "utility", "discovery", "community", "social", "support", "other"),
        World.OLD_WEST to listOf("discovery", "essential", "social", "utility", "support", "community", "other"),
        World.NIGHT_CITY to listOf("utility", "discovery", "essential", "social", "community", "support", "other"),
        World.PIXEL to listOf("discovery", "essential", "community", "utility", "support", "social", "other"),
        World.GREEN_TERMINAL to listOf("utility", "support", "essential", "community", "discovery", "social", "other"),
        World.SAN_ANDREAS to listOf("utility", "essential", "discovery", "social", "community", "support", "other"),
        World.HEAT to listOf("utility", "social", "essential", "discovery", "community", "support", "other"),
        World.RIVIA to listOf("discovery", "community", "essential", "support", "utility", "social", "other"),
        World.VICE_COAST to listOf("social", "discovery", "utility", "essential", "community", "support", "other"),
        World.LIBERTY to listOf("community", "utility", "essential", "discovery", "support", "social", "other"),
        World.CTOS to listOf("utility", "community", "discovery", "essential", "social", "support", "other"),
        World.HYRULE to listOf("discovery", "utility", "essential", "community", "support", "social", "other"),
        World.SKYRIM to listOf("community", "discovery", "essential", "utility", "social", "support", "other"),
        World.LANDS_BETWEEN to listOf("discovery", "essential", "community", "social", "utility", "support", "other"),
        World.HORIZON to listOf("utility", "discovery", "community", "essential", "support", "social", "other"),
        World.ASSASSIN to listOf("discovery", "social", "community", "essential", "utility", "support", "other"),
        World.FAR_CRY to listOf("community", "utility", "discovery", "essential", "social", "support", "other"),
        World.GHOST to listOf("discovery", "community", "utility", "essential", "support", "social", "other"),
        World.BRIDGES to listOf("utility", "support", "community", "essential", "discovery", "social", "other"),
        World.HORIZON_WILDS to listOf("discovery", "community", "support", "utility", "essential", "social", "other"),
        World.SPIDER to listOf("community", "discovery", "utility", "essential", "social", "support", "other"),
        World.HOGWARTS to listOf("discovery", "community", "essential", "social", "utility", "support", "other"),
        World.STARFIELD to listOf("utility", "support", "discovery", "community", "essential", "social", "other"),
        World.KINGDOM to listOf("community", "discovery", "essential", "social", "support", "utility", "other"),
        World.SAINTS to listOf("social", "utility", "discovery", "community", "essential", "support", "other"),
        World.JUST_CAUSE to listOf("utility", "discovery", "community", "support", "essential", "social", "other"),
        World.MAFIA to listOf("social", "community", "essential", "utility", "discovery", "support", "other"),
        World.DIVISION to listOf("utility", "support", "community", "discovery", "essential", "social", "other"),
        World.SEA_CHART to listOf("utility", "discovery", "support", "community", "essential", "social", "other"),
        World.DOS_ATLAS to listOf("support", "utility", "essential", "community", "discovery", "social", "other"),
        World.AINCRAD to listOf("discovery", "essential", "community", "utility", "social", "support", "other"),
        World.COUNTER_STRIKE to listOf("utility", "community", "support", "essential", "discovery", "social", "other"),
        World.DEFCON to listOf("community", "support", "utility", "essential", "discovery", "social", "other"),
        World.FIELD_OPS to listOf("utility", "community", "support", "discovery", "essential", "social", "other"),
        World.RETRO_QUEST to listOf("discovery", "essential", "utility", "community", "support", "social", "other"),
        World.HOME_CONSOLE to listOf("discovery", "social", "essential", "utility", "community", "support", "other"),
        World.STALKER to listOf("support", "community", "utility", "discovery", "essential", "social", "other"),
        World.NIER to listOf("support", "discovery", "community", "essential", "utility", "social", "other"),
    )

    private val layouts = buildMap {
        putAll(setOf(World.LOS_SANTOS, World.SAN_ANDREAS, World.VICE_COAST, World.LIBERTY,
            World.CTOS, World.SPIDER, World.SAINTS, World.MAFIA, World.NIGHT_CITY,
            World.HEAT, World.HORIZON).associateWith { WorldLegendLayout.GRID })
        putAll(setOf(World.OLD_WEST, World.RIVIA, World.HYRULE, World.SKYRIM,
            World.LANDS_BETWEEN, World.ASSASSIN, World.GHOST, World.HOGWARTS,
            World.KINGDOM, World.AINCRAD, World.RETRO_QUEST).associateWith { WorldLegendLayout.LEDGER })
        putAll(setOf(World.GREEN_TERMINAL, World.DIVISION, World.COUNTER_STRIKE,
            World.DEFCON, World.FIELD_OPS, World.STALKER, World.JUST_CAUSE,
            World.BRIDGES).associateWith { WorldLegendLayout.RADAR })
        putAll(setOf(World.PIXEL, World.STARFIELD, World.SEA_CHART, World.DOS_ATLAS,
            World.FAR_CRY, World.HORIZON_WILDS, World.HOME_CONSOLE, World.NIER).associateWith { WorldLegendLayout.TABLE })
    }

    private val groupKeys = listOf("discovery", "community", "essential", "support", "utility", "social", "other")
    private fun groupTitles(vararg labels: String): Map<String, String> {
        require(labels.size == groupKeys.size)
        return groupKeys.zip(labels).toMap()
    }

    /** Headings follow each game's map vocabulary while the rows still name real OSM categories. */
    private val headings = mapOf(
        World.LOS_SANTOS to groupTitles("CITY BLIPS", "CIVIC / TRANSIT", "QUICK STOPS", "REPAIR / SUPPORT", "VEHICLE SERVICES", "NIGHTLIFE", "OTHER PLACES"),
        World.OLD_WEST to groupTitles("TRAIL LANDMARKS", "TOWNS / RAIL", "TRAVEL NEEDS", "CAMP SUPPORT", "WAGON / FUEL", "SALOONS", "UNMARKED PLACES"),
        World.NIGHT_CITY to groupTitles("CITY NODES", "PUBLIC / TRANSIT", "PERSONAL SERVICES", "FIELD SUPPORT", "CHARGE / PARK", "AFTER HOURS", "UNINDEXED NODES"),
        World.PIXEL to groupTitles("BIOME STOPS", "VILLAGE SERVICES", "REST / RECOVERY", "CRAFT / INFO", "TRAVEL BLOCKS", "MEETUP SPOTS", "OTHER BLOCKS"),
        World.GREEN_TERMINAL to groupTitles("DATABASE ENTRIES", "CIVIC SYSTEMS", "CRITICAL SYSTEMS", "OPERATOR SUPPORT", "POWER / PARK", "NETWORK SOCIAL", "UNCLASSIFIED"),
        World.SAN_ANDREAS to groupTitles("COUNTY LOCATIONS", "CITY / TRANSIT", "ROAD ESSENTIALS", "GARAGE / REPAIR", "DRIVE SERVICES", "AFTER DARK", "OTHER BLIPS"),
        World.HEAT to groupTitles("SCENIC / SHOPS", "CITY / TRANSIT", "PIT STOPS", "ROAD SUPPORT", "GARAGES / FUEL", "MEETUPS", "OTHER STOPS"),
        World.RIVIA to groupTitles("TRAVELER LANDMARKS", "TOWNS / TRANSIT", "TRAVELER NEEDS", "TRAIL SUPPORT", "ROAD SERVICES", "TAVERNS", "OTHER PLACES"),
        World.VICE_COAST to groupTitles("COASTAL PLACES", "CITY / TRANSIT", "ESSENTIAL STOPS", "ROAD SUPPORT", "PARK / FUEL", "NIGHT SCENE", "OTHER PLACES"),
        World.LIBERTY to groupTitles("BOROUGH POINTS", "CIVIC / TRANSIT", "CITY ESSENTIALS", "REPAIR / SERVICES", "PARK / FUEL", "NIGHTLIFE", "OTHER PLACES"),
        World.CTOS to groupTitles("CONNECTED PLACES", "PUBLIC NETWORK", "ESSENTIAL NODES", "DEVICE SUPPORT", "CHARGING / PARKING", "SOCIAL NODES", "OTHER MAP NODES"),
        World.HYRULE to groupTitles("LANDMARKS / MARKETS", "VILLAGE / TRANSIT", "REST / CARE", "TRAVEL SUPPORT", "ROUTE SERVICES", "INNS / GATHERING", "OTHER PLACES"),
        World.SKYRIM to groupTitles("HOLD LANDMARKS", "HOLDS / TRANSIT", "TRAVEL NEEDS", "TRAIL SUPPORT", "ROAD SERVICES", "MEAD HALLS", "OTHER PLACES"),
        World.LANDS_BETWEEN to groupTitles("SITES / LANDMARKS", "CIVIC / TRANSIT", "REST / AID", "FIELD SUPPORT", "ROUTE SERVICES", "SOCIAL SITES", "OTHER PLACES"),
        World.HORIZON to groupTitles("ROAD DESTINATIONS", "CITY / TRANSIT", "PIT STOPS", "RALLY SUPPORT", "VEHICLE SERVICES", "MEETUPS", "OTHER PLACES"),
        World.ASSASSIN to groupTitles("DISCOVERY SITES", "SETTLEMENT / TRANSIT", "TRAVEL SUPPLIES", "FIELD SUPPORT", "ROAD SERVICES", "MARKET / SOCIAL", "OTHER PLACES"),
        World.FAR_CRY to groupTitles("COUNTY PLACES", "TOWNS / TRANSIT", "ESSENTIAL STOPS", "FIELD SUPPORT", "ROAD SERVICES", "LOCAL HANGOUTS", "OTHER PLACES"),
        World.GHOST to groupTitles("WAYPOINT PLACES", "SETTLEMENT / TRANSIT", "TRAVEL NEEDS", "FIELD SUPPORT", "ROAD STOPS", "TEA / REST", "OTHER PLACES"),
        World.BRIDGES to groupTitles("LANDMARKS / PLACES", "CITY / TRANSIT", "TRAVEL NEEDS", "FIELD SERVICES", "ROUTE INFRASTRUCTURE", "REST POINTS", "OTHER PLACES"),
        World.HORIZON_WILDS to groupTitles("LANDMARKS / SERVICES", "SETTLEMENT / TRANSIT", "TRAVEL NEEDS", "FIELD SUPPORT", "ROUTE SERVICES", "CAMPS / SOCIAL", "OTHER PLACES"),
        World.SPIDER to groupTitles("CITY LANDMARKS", "DISTRICT / TRANSIT", "CITY ESSENTIALS", "SUPPORT SERVICES", "TRAVEL / PARKING", "SOCIAL / NIGHT", "OTHER PLACES"),
        World.HOGWARTS to groupTitles("HIGHLANDS / SITES", "TOWNS / TRANSIT", "TRAVEL NEEDS", "FIELD SUPPORT", "ROUTE STOPS", "INNS / SOCIAL", "OTHER PLACES"),
        World.STARFIELD to groupTitles("SURVEY DESTINATIONS", "SETTLEMENT SERVICES", "LIFE SUPPORT", "FIELD SERVICES", "ROUTE SUPPORT", "HAB / SOCIAL", "UNCLASSIFIED"),
        World.KINGDOM to groupTitles("BOHEMIAN SITES", "TOWNS / RAIL", "TRAVEL NEEDS", "FIELD SUPPORT", "ROAD SERVICES", "TAVERN / SOCIAL", "OTHER PLACES"),
        World.SAINTS to groupTitles("CITY ATTRACTIONS", "CIVIC / TRANSIT", "QUICK STOPS", "GARAGE / HELP", "ROAD SERVICES", "NIGHTLIFE", "OTHER PLACES"),
        World.JUST_CAUSE to groupTitles("ISLAND SITES", "TOWNS / TRANSIT", "ESSENTIAL STOPS", "FIELD SUPPORT", "ROAD / FUEL", "COAST / SOCIAL", "OTHER PLACES"),
        World.MAFIA to groupTitles("LOST HEAVEN SITES", "CITY / RAIL", "ESSENTIAL STOPS", "GARAGE / SUPPORT", "ROAD SERVICES", "SOCIAL CLUBS", "OTHER PLACES"),
        World.DIVISION to groupTitles("FIELD LOCATIONS", "CIVIC / TRANSIT", "SURVIVAL STOPS", "FIELD SUPPORT", "ROUTE SERVICES", "SAFE REST", "OTHER PLACES"),
        World.SEA_CHART to groupTitles("COASTAL LANDMARKS", "PORT / TRANSIT", "CREW NEEDS", "CHART SUPPORT", "FUEL / PARK", "HARBOR SOCIAL", "OTHER PLACES"),
        World.DOS_ATLAS to groupTitles("ATLAS ENTRIES", "SYSTEM SERVICES", "ESSENTIALS", "TOOLS / SUPPORT", "ROUTE SYSTEM", "COMMUNICATIONS", "UNMAPPED ENTRIES"),
        World.AINCRAD to groupTitles("FIELD SITES", "SETTLEMENT / TRANSIT", "TRAVEL NEEDS", "FIELD SUPPORT", "ROUTE SERVICES", "GUILD / SOCIAL", "OTHER PLACES"),
        World.COUNTER_STRIKE to groupTitles("RADAR POINTS", "PUBLIC / TRANSIT", "TEAM ESSENTIALS", "SUPPORT POINTS", "MOVEMENT SERVICES", "TEAM MEETUPS", "OTHER POINTS"),
        World.DEFCON to groupTitles("STRATEGIC SITES", "CIVIC / TRANSIT", "SURVIVAL / AID", "SUPPORT NODES", "ROUTE SERVICES", "COMMUNICATIONS", "UNCLASSIFIED"),
        World.FIELD_OPS to groupTitles("AREA PLACES", "CIVIC / TRANSIT", "MISSION ESSENTIALS", "FIELD SUPPORT", "ROUTE SERVICES", "SAFE REST", "OTHER PLACES"),
        World.RETRO_QUEST to groupTitles("OVERWORLD PLACES", "TOWNS / TRANSIT", "HEAL / REST", "INVENTORY SUPPORT", "TRAVEL LINKS", "PARTY / SOCIAL", "OTHER PLACES"),
        World.NIER to groupTitles("CITY LANDMARKS", "PUBLIC / TRANSIT", "SUPPLY STOPS", "FIELD SUPPORT", "ROUTE SERVICES", "CAMP / SOCIAL", "OTHER PLACES"),
        World.STALKER to groupTitles("FIELD SITES", "SETTLEMENT / TRANSIT", "SUPPLY / CARE", "FIELD SUPPORT", "ROUTE SERVICES", "CAMP / SOCIAL", "OTHER OBSERVED"),
        World.HOME_CONSOLE to groupTitles("PLACES TO GO", "COMMUNITY LINKS", "ESSENTIAL STOPS", "HELPFUL PLACES", "GETTING AROUND", "MEETUP SPOTS", "OTHER PLACES"),
    )

    private val navigationLabels = mapOf(
        World.LOS_SANTOS to MapLegendNavigationLabels("GPS LINE", "PLAYER BLIP", "MARKED DESTINATION"),
        World.OLD_WEST to MapLegendNavigationLabels("RIDING TRAIL", "RIDER POSITION", "MARKED WAYPOINT"),
        World.NIGHT_CITY to MapLegendNavigationLabels("NAV TRACE", "POSITION PIP", "TARGET LOCATION"),
        World.PIXEL to MapLegendNavigationLabels("PATH", "PLAYER BLOCK", "TARGET BLOCK"),
        World.GREEN_TERMINAL to MapLegendNavigationLabels("ROUTE VECTOR", "OPERATOR FIX", "TARGET NODE"),
        World.SAN_ANDREAS to MapLegendNavigationLabels("ROAD ROUTE", "PLAYER MARKER", "PINNED STOP"),
        World.HEAT to MapLegendNavigationLabels("RACE LINE", "VEHICLE FIX", "PINNED STOP"),
        World.RIVIA to MapLegendNavigationLabels("TRAVEL LINE", "TRAVELER BEARING", "MARKED STOP"),
        World.VICE_COAST to MapLegendNavigationLabels("COAST ROUTE", "VEHICLE FIX", "PINNED STOP"),
        World.LIBERTY to MapLegendNavigationLabels("ROUTE LINE", "PLAYER POSITION", "SELECTED DESTINATION"),
        World.CTOS to MapLegendNavigationLabels("ROUTE PATH", "LIVE POSITION", "TARGET PLACE"),
        World.HYRULE to MapLegendNavigationLabels("TRAVEL PATH", "TRAVELER POSITION", "DESTINATION PIN"),
        World.SKYRIM to MapLegendNavigationLabels("TRAVEL LINE", "TRAVELER BEARING", "MARKED PLACE"),
        World.LANDS_BETWEEN to MapLegendNavigationLabels("GUIDANCE TRACE", "TRAVELER POSITION", "DESTINATION MARK"),
        World.HORIZON to MapLegendNavigationLabels("RACE ROUTE", "VEHICLE BEARING", "PINNED DESTINATION"),
        World.ASSASSIN to MapLegendNavigationLabels("TRAVEL LINE", "TRAVELER POSITION", "MARKED LOCATION"),
        World.FAR_CRY to MapLegendNavigationLabels("ROAD ROUTE", "DRIVER POSITION", "PINNED STOP"),
        World.GHOST to MapLegendNavigationLabels("GUIDING ROUTE", "TRAVELER POSITION", "WAYPOINT MARK"),
        World.BRIDGES to MapLegendNavigationLabels("GUIDANCE TRACE", "PORTER POSITION", "DESTINATION NODE"),
        World.HORIZON_WILDS to MapLegendNavigationLabels("TRAVEL TRACE", "TRAVELER POSITION", "MARKED STOP"),
        World.SPIDER to MapLegendNavigationLabels("ROUTE LINE", "WEB-HEAD POSITION", "DESTINATION PIN"),
        World.HOGWARTS to MapLegendNavigationLabels("HIGHLANDS ROUTE", "HIGHLANDS POSITION", "PINNED HIGHLANDS STOP"),
        World.STARFIELD to MapLegendNavigationLabels("SURVEY ROUTE", "CREW POSITION", "SELECTED SITE"),
        World.KINGDOM to MapLegendNavigationLabels("TRAVEL LINE", "RIDER POSITION", "MARKED WAYPOINT"),
        World.SAINTS to MapLegendNavigationLabels("ROUTE LINE", "PLAYER VEHICLE", "MARKED DESTINATION"),
        World.JUST_CAUSE to MapLegendNavigationLabels("ISLAND ROUTE", "PILOT POSITION", "MARKED LOCATION"),
        World.MAFIA to MapLegendNavigationLabels("ROAD LINE", "DRIVER POSITION", "MARKED DESTINATION"),
        World.DIVISION to MapLegendNavigationLabels("ROUTE TRACE", "AGENT POSITION", "SELECTED LOCATION"),
        World.SEA_CHART to MapLegendNavigationLabels("SEA ROUTE", "VESSEL POSITION", "CHARTED DESTINATION"),
        World.DOS_ATLAS to MapLegendNavigationLabels("ROUTE TRACE", "COORDINATE FIX", "TARGET LOCATION"),
        World.AINCRAD to MapLegendNavigationLabels("FIELD ROUTE", "TRAVELER POSITION", "DESTINATION PIN"),
        World.COUNTER_STRIKE to MapLegendNavigationLabels("ROUTE VECTOR", "POSITION MARK", "MARKED SITE"),
        World.DEFCON to MapLegendNavigationLabels("COMMAND ROUTE", "UNIT POSITION", "SELECTED SITE"),
        World.FIELD_OPS to MapLegendNavigationLabels("ROUTE LINE", "OPERATOR POSITION", "MARKED LOCATION"),
        World.RETRO_QUEST to MapLegendNavigationLabels("WORLD PATH", "PLAYER POSITION", "MARKED DESTINATION"),
        World.NIER to MapLegendNavigationLabels("ROUTE TRACE", "POD BEARING", "TARGET LOCATION"),
        World.STALKER to MapLegendNavigationLabels("ROUTE TRACE", "STALKER POSITION", "MARKED SITE"),
        World.HOME_CONSOLE to MapLegendNavigationLabels("ROUTE LINE", "PLAYER POSITION", "DESTINATION"),
    )

    init {
        check(profiles.keys == World.entries.toSet()) { "Every world needs its own map-key vocabulary." }
        check(groupOrders.keys == World.entries.toSet() && layouts.keys == World.entries.toSet()) {
            "Every world needs a complete map-key group order and layout."
        }
        check(headings.keys == World.entries.toSet() && navigationLabels.keys == World.entries.toSet()) {
            "Every world needs unique map-key section names and navigation symbols."
        }
        val groups = setOf("discovery", "community", "essential", "support", "utility", "social", "other")
        check(groupOrders.values.all { it.toSet() == groups && it.size == groups.size }) {
            "Every world must show all map-key groups exactly once."
        }
        check(groupOrders.values.toSet().size == World.entries.size) {
            "Each world needs its own map-key group hierarchy."
        }
        check(headings.values.all { it.keys == groups && it.values.size == groups.size }) {
            "Each world's map key must label every live place category group."
        }
        check(headings.values.map { it.values.toSet() }.toSet().size == World.entries.size) {
            "Every world needs a distinct set of map-key section titles."
        }
        check(navigationLabels.values.toSet().size == World.entries.size) {
            "Each world needs distinct names for its route, position, and destination marks."
        }
    }

    fun forWorld(world: World): WorldLegendProfile = profiles.getValue(world).copy(
        groupOrder = groupOrders.getValue(world),
        layout = layouts.getValue(world),
        groupTitles = headings.getValue(world),
        navigation = navigationLabels.getValue(world),
    )
}
