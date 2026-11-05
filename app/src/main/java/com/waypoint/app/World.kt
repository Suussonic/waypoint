package com.waypoint.app

import androidx.compose.ui.graphics.Color

enum class World(val title: String,val subtitle: String,val descriptionRes: Int,val file: String,val accent: Color,val routeColor: String,val ground: Color,val road: Color,val building: Color) {
    LOS_SANTOS("LOS SANTOS","THE CITY IS YOURS",R.string.world_los_santos_description,"los_santos",Color(0xFFB6DB91),"#B786ED",Color(0xFF20221F),Color(0xFF73776C),Color(0xFF383B35)),
    OLD_WEST("OLD WEST","TAKE THE LONG WAY",R.string.world_old_west_description,"old_west",Color(0xFFE4BE82),"#B52E31",Color(0xFFDAC8A2),Color(0xFF665F4B),Color(0xFFC6B18D)),
    NIGHT_CITY("NIGHT CITY","AFTER HOURS",R.string.world_night_city_description,"night_city",Color(0xFF6DE9DD),"#F6E75C",Color(0xFF0C1019),Color(0xFF258DA3),Color(0xFF8B2029)),
    PIXEL("PIXEL WORLD","A NEW PERSPECTIVE",R.string.world_pixel_description,"pixel",Color(0xFFB8E088),"#E95345",Color(0xFF566943),Color(0xFFC7C4A5),Color(0xFF777D6C)),
    GREEN_TERMINAL("GREEN TERMINAL","SIGNAL FOUND",R.string.world_green_terminal_description,"green_terminal",Color(0xFF9AF477),"#D5FF9B",Color(0xFF071109),Color(0xFF6BCF55),Color(0xFF1E4922)),
    SAN_ANDREAS("SAN ANDREAS","BACK TO THE STREETS",R.string.world_san_andreas_description,"san_andreas",Color(0xFFF2C877),"#DA5B3C",Color(0xFFC2C5AD),Color(0xFFF4F0D8),Color(0xFFA4A48E)),
    HEAT("MIDNIGHT RUN","TAKE THE NIGHT SHIFT",R.string.world_heat_description,"heat",Color(0xFFFFA9E8),"#75EBF5",Color(0xFF555946),Color(0xFFD2D2B6),Color(0xFF6A6A5A)),
    RIVIA("THE CONTINENT","The Witcher 3",R.string.world_rivia_description,"rivia",Color(0xFFD6D59A),"#9F3435",Color(0xFFB6AF8D),Color(0xFFE4D6AE),Color(0xFF93886B)),
    VICE_COAST("VICE COAST","SUNSET STREETS",R.string.world_vice_coast_description,"vice_coast",Color(0xFFFFAFE1),"#F46AC0",Color(0xFF94909D),Color(0xFFF6F1DD),Color(0xFFB4B4B0)),
    LIBERTY("LIBERTY CITY","THE CONCRETE GRID",R.string.world_liberty_description,"liberty",Color(0xFFFFD06D),"#EFA937",Color(0xFF222527),Color(0xFF92999C),Color(0xFF42484B)),
    CTOS("NUDLE MAPS","CONNECTED CITY",R.string.world_ctos_description,"ctos",Color(0xFF78C9FF),"#F28D31",Color(0xFFE5E9DD),Color(0xFFFFFFFF),Color(0xFFC8CED0)),
    HYRULE("SHEIKAH SLATE","THE EXPLORER’S MAP",R.string.world_hyrule_description,"hyrule",Color(0xFF81D9FF),"#F0CE6B",Color(0xFF4B4932),Color(0xFFA69C64),Color(0xFF5F5B3B)),
    SKYRIM("SKYRIM","THE NORTHERN ROAD",R.string.world_skyrim_description,"skyrim",Color(0xFFDCE9EE),"#E9E4CE",Color(0xFF859395),Color(0xFFBDC4BE),Color(0xFF647577)),
    LANDS_BETWEEN("LANDS BETWEEN","A GOLDEN THREAD",R.string.world_lands_between_description,"lands_between",Color(0xFFE3CC8D),"#EAC968",Color(0xFFB1A789),Color(0xFFE0D1AA),Color(0xFF817963)),
    HORIZON("HORIZON","THE OPEN ROAD",R.string.world_horizon_description,"horizon",Color(0xFFFF92D6),"#51C4F1",Color(0xFF8A966A),Color(0xFFF5F0DD),Color(0xFFA3A18A)),
    ASSASSIN("ANIMUS","Assassin’s Creed Origins",R.string.world_assassin_description,"assassin",Color(0xFFEBD28C),"#e7c65d",Color(0xFFB49B6E),Color(0xFFE7D6AD),Color(0xFFA49477)),
    FAR_CRY("HOPE COUNTY","Far Cry 5",R.string.world_far_cry_description,"far_cry",Color(0xFFF4D57B),"#eac35b",Color(0xFF6E7A60),Color(0xFFDDD9BC),Color(0xFF818777)),
    GHOST("TSUSHIMA","Ghost of Tsushima",R.string.world_ghost_description,"ghost",Color(0xFFE8E5D7),"#b84435",Color(0xFFC9CCC2),Color(0xFFEEEADE),Color(0xFFAEB4A8)),
    BRIDGES("BRIDGES","Death Stranding",R.string.world_bridges_description,"bridges",Color(0xFF9AE5DD),"#d8c87a",Color(0xFF182B37),Color(0xFF86AAB1),Color(0xFF344953)),
    HORIZON_WILDS("THE EMBRACE","Horizon Zero Dawn",R.string.world_horizon_wilds_description,"horizon_wilds",Color(0xFFF0DBAC),"#f1d784",Color(0xFF5B6251),Color(0xFFC6B88C),Color(0xFF707365)),
    SPIDER("MANHATTAN","Marvel’s Spider-Man",R.string.world_spider_description,"spider",Color(0xFFFF9E9D),"#f6d156",Color(0xFF102A38),Color(0xFF4290A4),Color(0xFF1B6173)),
    HOGWARTS("HIGHLANDS","Hogwarts Legacy",R.string.world_hogwarts_description,"hogwarts",Color(0xFFE5CE93),"#dabd69",Color(0xFF77765F),Color(0xFFC4BEA0),Color(0xFF72765E)),
    STARFIELD("SURFACE MAP","Starfield",R.string.world_starfield_description,"starfield",Color(0xFFEEF1E9),"#f3c56f",Color(0xFF1B304B),Color(0xFF7196AF),Color(0xFF36546A)),
    KINGDOM("BOHEMIA","Kingdom Come: Deliverance II",R.string.world_kingdom_description,"kingdom",Color(0xFFE6CF94),"#dfba52",Color(0xFFB2A37D),Color(0xFFE7D4AA),Color(0xFFA39472)),
    SAINTS("STEELPORT","Saints Row: The Third",R.string.world_saints_description,"saints",Color(0xFFD0A6FB),"#b893f3",Color(0xFF1B1C22),Color(0xFF7C8490),Color(0xFF363740)),
    JUST_CAUSE("MEDICI","Just Cause 3",R.string.world_just_cause_description,"just_cause",Color(0xFF87ECDF),"#f0cf6a",Color(0xFF143748),Color(0xFF6FA4AD),Color(0xFF305360)),
    MAFIA("LOST HEAVEN","Mafia: Definitive Edition",R.string.world_mafia_description,"mafia",Color(0xFFE8D29A),"#bd624c",Color(0xFFC1C1A4),Color(0xFFE8D174),Color(0xFFAAA99A)),
    DIVISION("DARK ZONE","FIND YOUR BEARING",R.string.world_division_description,"division",Color(0xFFFFAE61),"#FF9D3D",Color(0xFF182C2E),Color(0xFF819A98),Color(0xFF355152)),
    SEA_CHART("SEA CHART","CHART YOUR COURSE",R.string.world_sea_chart_description,"sea_chart",Color(0xFFE7D29C),"#A13C32",Color(0xFFD8CBAB),Color(0xFFF2E6C7),Color(0xFFACA080)),
    DOS_ATLAS("DOS ATLAS","THE WORLD, IN FOCUS",R.string.world_dos_atlas_description,"dos_atlas",Color(0xFFFFD18A),"#FFE4A0",Color(0xFF15100D),Color(0xFFB38D52),Color(0xFF4C3D28)),
    AINCRAD("AINCRAD","A NEW CHAPTER",R.string.world_aincrad_description,"aincrad",Color(0xFFF0C862),"#D47D29",Color(0xFFCEBC92),Color(0xFFF7EACB),Color(0xFFB3A78B)),
    COUNTER_STRIKE("TACTICAL RADAR","KNOW YOUR SURROUNDINGS",R.string.world_counter_strike_description,"counter_strike",Color(0xFFFFD37F),"#67CBEE",Color(0xFF302F2A),Color(0xFFD6D2BD),Color(0xFF65635B)),
    DEFCON("VECTOR COMMAND","A WIDER PERSPECTIVE",R.string.world_defcon_description,"defcon",Color(0xFF9AC8FF),"#FFFFFF",Color(0xFF030815),Color(0xFF5D9BD4),Color(0xFF112449)),
    FIELD_OPS("FIELD OPS","PLAN THE NEXT MOVE",R.string.world_field_ops_description,"field_ops",Color(0xFFCDDCAC),"#F1E9A5",Color(0xFF31382F),Color(0xFFBBC2A8),Color(0xFF535D4E)),
    RETRO_QUEST("16-BIT QUEST","THE NEXT ADVENTURE",R.string.world_retro_quest_description,"retro_quest",Color(0xFFFFEBA3),"#E56470",Color(0xFF88A95C),Color(0xFFE5CA89),Color(0xFFA2856F)),
    HOME_CONSOLE("HOME CONSOLE","LET’S GO SOMEWHERE",R.string.world_home_console_description,"home_console",Color(0xFF78CBE8),"#2185BA",Color(0xFFE8EDF0),Color(0xFFFFFFFF),Color(0xFFC5CFD7)),
    STALKER("EXCLUSION ZONE","YOUR PERSONAL FIELD MAP",R.string.world_stalker_description,"stalker",Color(0xFFE6C274),"#9EE376",Color(0xFF3A3F31),Color(0xFFB6B39A),Color(0xFF66695D)),
    NIER("MACHINE ATLAS","A WORLD TO DISCOVER",R.string.world_nier_description,"nier",Color(0xFFD3C9A4),"#A14F37",Color(0xFFCCC8B0),Color(0xFFE5E1CD),Color(0xFF9D9B88)),
}

val newWorlds = setOf(World.DIVISION, World.SEA_CHART, World.DOS_ATLAS, World.AINCRAD, World.COUNTER_STRIKE, World.DEFCON, World.FIELD_OPS, World.RETRO_QUEST, World.HOME_CONSOLE, World.STALKER, World.NIER)

fun World.hasTerrain()=this in setOf(World.HEAT,World.RIVIA,World.HYRULE,World.SKYRIM,World.LANDS_BETWEEN,World.HORIZON,
    World.ASSASSIN,World.FAR_CRY,World.GHOST,World.BRIDGES,World.HORIZON_WILDS,World.HOGWARTS,World.STARFIELD,World.KINGDOM,World.JUST_CAUSE)
