package com.waypoint.app

import androidx.annotation.DrawableRes

/** Original, theme-specific Waypoint emblems used in the picker and map HUD. */
@DrawableRes
fun worldEmblemFor(world: World): Int = when (world) {
    World.LOS_SANTOS -> R.drawable.world_emblem_los_santos
    World.OLD_WEST -> R.drawable.world_emblem_old_west
    World.PIXEL -> R.drawable.world_emblem_pixel
    World.NIGHT_CITY -> R.drawable.world_emblem_night_city
    World.GREEN_TERMINAL -> R.drawable.world_emblem_green_terminal
    World.SAN_ANDREAS -> R.drawable.world_emblem_san_andreas
    World.HEAT -> R.drawable.world_emblem_heat
    World.RIVIA -> R.drawable.world_emblem_rivia
    World.VICE_COAST -> R.drawable.world_emblem_vice_coast
    World.LIBERTY -> R.drawable.world_emblem_liberty
    World.CTOS -> R.drawable.world_emblem_ctos
    World.HYRULE -> R.drawable.world_emblem_hyrule
    World.SKYRIM -> R.drawable.world_emblem_skyrim
    World.LANDS_BETWEEN -> R.drawable.world_emblem_lands_between
    World.HORIZON -> R.drawable.world_emblem_horizon
    World.ASSASSIN -> R.drawable.world_emblem_assassin
    World.FAR_CRY -> R.drawable.world_emblem_far_cry
    World.GHOST -> R.drawable.world_emblem_ghost
    World.BRIDGES -> R.drawable.world_emblem_bridges
    World.HORIZON_WILDS -> R.drawable.world_emblem_horizon_wilds
    World.SPIDER -> R.drawable.world_emblem_spider
    World.HOGWARTS -> R.drawable.world_emblem_hogwarts
    World.STARFIELD -> R.drawable.world_emblem_starfield
    World.KINGDOM -> R.drawable.world_emblem_kingdom
    World.SAINTS -> R.drawable.world_emblem_saints
    World.JUST_CAUSE -> R.drawable.world_emblem_just_cause
    World.MAFIA -> R.drawable.world_emblem_mafia
}
