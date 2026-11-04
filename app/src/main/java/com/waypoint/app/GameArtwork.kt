package com.waypoint.app

import androidx.annotation.DrawableRes

/** Original Waypoint world illustrations used by the first-run gallery. */
@DrawableRes
fun artworkFor(world: World): Int = when (world) {
    World.LOS_SANTOS -> R.drawable.art_los_santos
    World.OLD_WEST -> R.drawable.art_old_west
    World.NIGHT_CITY -> R.drawable.art_night_city
    World.PIXEL -> R.drawable.art_pixel
    World.GREEN_TERMINAL -> R.drawable.art_green_terminal
    World.SAN_ANDREAS -> R.drawable.art_san_andreas
    World.HEAT -> R.drawable.art_heat
    World.RIVIA -> R.drawable.art_rivia
    World.VICE_COAST -> R.drawable.art_vice_coast
    World.LIBERTY -> R.drawable.art_liberty
    World.CTOS -> R.drawable.art_ctos
    World.HYRULE -> R.drawable.art_hyrule
    World.SKYRIM -> R.drawable.art_skyrim
    World.LANDS_BETWEEN -> R.drawable.art_lands_between
    World.HORIZON -> R.drawable.art_horizon
    World.ASSASSIN -> R.drawable.art_assassin
    World.FAR_CRY -> R.drawable.art_far_cry
    World.GHOST -> R.drawable.art_ghost
    World.BRIDGES -> R.drawable.art_bridges
    World.HORIZON_WILDS -> R.drawable.art_horizon_wilds
    World.SPIDER -> R.drawable.art_spider
    World.HOGWARTS -> R.drawable.art_hogwarts
    World.STARFIELD -> R.drawable.art_starfield
    World.KINGDOM -> R.drawable.art_kingdom
    World.SAINTS -> R.drawable.art_saints
    World.JUST_CAUSE -> R.drawable.art_just_cause
    World.MAFIA -> R.drawable.art_mafia
    World.DIVISION -> R.drawable.preview_division
    World.SEA_CHART -> R.drawable.preview_sea_chart
    World.DOS_ATLAS -> R.drawable.preview_dos_atlas
    World.AINCRAD -> R.drawable.preview_aincrad
    World.COUNTER_STRIKE -> R.drawable.preview_counter_strike
    World.DEFCON -> R.drawable.preview_defcon
    World.FIELD_OPS -> R.drawable.preview_field_ops
    World.RETRO_QUEST -> R.drawable.preview_retro_quest
    World.NIER -> R.drawable.preview_nier
    World.STALKER -> R.drawable.preview_stalker
    World.HOME_CONSOLE -> R.drawable.preview_home_console
}
