package com.waypoint.app

/** Bound every level to the host's row budget without dropping destinations. */
internal fun <T> carMenuGroups(items: List<T>, limit: Int): List<List<T>> {
    require(limit >= 2)
    if(items.isEmpty()) return emptyList()
    val groupSize = if(items.size <= limit) 1 else (items.size + limit - 1) / limit
    return items.chunked(groupSize)
}

internal val carWorldGroups = linkedMapOf(
    R.string.car_world_cities to listOf(World.LOS_SANTOS, World.SAN_ANDREAS, World.VICE_COAST, World.LIBERTY, World.CTOS, World.SAINTS),
    R.string.car_world_roads to listOf(World.OLD_WEST, World.HEAT, World.HORIZON, World.MAFIA, World.FAR_CRY, World.JUST_CAUSE),
    R.string.car_world_fantasy to listOf(World.RIVIA, World.HYRULE, World.SKYRIM, World.LANDS_BETWEEN, World.HOGWARTS, World.KINGDOM),
    R.string.car_world_future to listOf(World.NIGHT_CITY, World.BRIDGES, World.HORIZON_WILDS, World.SPIDER, World.STARFIELD, World.AINCRAD, World.NIER),
    R.string.car_world_tactical to listOf(World.ASSASSIN, World.GHOST, World.DIVISION, World.COUNTER_STRIKE, World.DEFCON, World.FIELD_OPS, World.STALKER),
    R.string.car_world_classic to listOf(World.PIXEL, World.GREEN_TERMINAL, World.DOS_ATLAS, World.SEA_CHART, World.RETRO_QUEST, World.HOME_CONSOLE)
)
