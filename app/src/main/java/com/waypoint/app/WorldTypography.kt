package com.waypoint.app

import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontFamily

/** Phone Compose and projected Canvas HUDs share the same title and reading roles. */
internal enum class WorldTypeface(@FontRes val regular: Int,@FontRes val bold: Int=regular) {
    DISPLAY(R.font.barlow_condensed_bold),
    WESTERN(R.font.kirsty_regular,R.font.kirsty_bold),
    WESTERN_TITLE(R.font.chinese_rocks_regular),
    GTA_TITLE(R.font.pricedown_black),
    PIXEL(R.font.minecraft_seven),
    RETRO_PIXEL(R.font.monocraft),
    BORDA(R.font.borda_regular,R.font.borda_bold),
    STRATUM(R.font.stratum2_regular,R.font.stratum2_bold),
    WINDLASS(R.font.windlass),
    FUTURA_CONDENSED(R.font.futura_condensed_medium),
    TERMINAL(R.font.share_tech_mono),
    TECHNICAL(R.font.rajdhani),
    ATLAS_TITLE(R.font.cinzel),
    PIP_BOY(R.font.roboto_condensed),
    READING_SERIF(R.font.libre_baskerville),
    SANS(R.font.noto_sans),
    VICE_TITLE(R.font.yellowtail);

    val family: FontFamily get()=when(this) {
        DISPLAY -> DisplayFont
        WESTERN -> WesternFont
        WESTERN_TITLE -> WesternDisplayFont
        GTA_TITLE -> GtaDisplayFont
        PIXEL -> PixelFont
        RETRO_PIXEL -> RetroPixelFont
        BORDA -> BordaFont
        STRATUM -> StratumFont
        WINDLASS -> WindlassFont
        FUTURA_CONDENSED -> FuturaCondensedFont
        TERMINAL -> TerminalFont
        TECHNICAL -> TechnicalFont
        ATLAS_TITLE -> AtlasFont
        PIP_BOY -> PipBoyFont
        READING_SERIF -> ReadingSerifFont
        SANS -> SansFont
        VICE_TITLE -> ViceFont
    }
}

internal fun World.titleTypeface(): WorldTypeface=when(this) {
    World.DIVISION -> WorldTypeface.BORDA
    World.COUNTER_STRIKE -> WorldTypeface.STRATUM
    World.SEA_CHART -> WorldTypeface.WINDLASS
    World.SKYRIM -> WorldTypeface.FUTURA_CONDENSED
    World.FIELD_OPS,World.NIGHT_CITY,World.BRIDGES,World.SPIDER,World.STARFIELD -> WorldTypeface.TECHNICAL
    World.DOS_ATLAS,World.DEFCON -> WorldTypeface.TERMINAL
    World.PIXEL -> WorldTypeface.PIXEL
    World.RETRO_QUEST -> WorldTypeface.RETRO_PIXEL
    World.LANDS_BETWEEN,World.HYRULE,World.ASSASSIN,World.HORIZON_WILDS,
    World.HOGWARTS,World.KINGDOM,World.MAFIA -> WorldTypeface.ATLAS_TITLE
    World.AINCRAD,World.HOME_CONSOLE,World.NIER -> WorldTypeface.SANS
    World.VICE_COAST -> WorldTypeface.VICE_TITLE
    World.LOS_SANTOS,World.SAN_ANDREAS,World.LIBERTY -> WorldTypeface.GTA_TITLE
    World.OLD_WEST -> WorldTypeface.WESTERN_TITLE
    World.RIVIA -> WorldTypeface.READING_SERIF
    World.GREEN_TERMINAL,World.STALKER -> WorldTypeface.PIP_BOY
    else -> WorldTypeface.DISPLAY
}

internal fun World.readingTypeface(): WorldTypeface=when(this) {
    World.DOS_ATLAS,World.DEFCON -> WorldTypeface.TERMINAL
    World.PIXEL -> WorldTypeface.PIXEL
    World.RETRO_QUEST -> WorldTypeface.RETRO_PIXEL
    World.DIVISION -> WorldTypeface.BORDA
    World.COUNTER_STRIKE -> WorldTypeface.STRATUM
    World.SKYRIM -> WorldTypeface.FUTURA_CONDENSED
    World.FIELD_OPS,World.NIGHT_CITY,World.BRIDGES,World.SPIDER,World.STARFIELD -> WorldTypeface.TECHNICAL
    World.OLD_WEST -> WorldTypeface.WESTERN
    World.GREEN_TERMINAL,World.STALKER -> WorldTypeface.PIP_BOY
    World.SEA_CHART,World.LANDS_BETWEEN,World.RIVIA,World.KINGDOM,World.HOGWARTS,World.MAFIA -> WorldTypeface.READING_SERIF
    World.AINCRAD,World.HOME_CONSOLE,World.NIER,World.CTOS,World.HORIZON,World.SAINTS -> WorldTypeface.SANS
    else -> WorldTypeface.DISPLAY
}
