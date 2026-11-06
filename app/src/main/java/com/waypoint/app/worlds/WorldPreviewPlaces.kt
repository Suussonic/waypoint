package com.waypoint.app

import androidx.annotation.DrawableRes

/** Real Indian streets chosen to suit each world's visual character. */
data class WorldPreviewPlace(val label: String, val lat: Double, val lon: Double, val zoom: Double)

fun World.previewPlace(): WorldPreviewPlace = when (this) {
    World.LOS_SANTOS -> WorldPreviewPlace("Mumbai · Marine Drive", 18.9440, 72.8240, 14.6)
    World.OLD_WEST -> WorldPreviewPlace("Jaisalmer · Fort", 26.9120, 70.9150, 15.2)
    World.GREEN_TERMINAL -> WorldPreviewPlace("Kolkata · Esplanade", 22.5660, 88.3510, 15.6)
    World.PIXEL -> WorldPreviewPlace("Jaipur · Old City", 26.9260, 75.8280, 15.7)
    World.NIGHT_CITY -> WorldPreviewPlace("Gurugram · Cyber Hub", 28.4950, 77.0890, 16.0)
    World.SAN_ANDREAS -> WorldPreviewPlace("Chennai · Marina", 13.0490, 80.2820, 14.8)
    World.HEAT -> WorldPreviewPlace("Mumbai · BKC", 19.0660, 72.8680, 15.6)
    World.RIVIA -> WorldPreviewPlace("Darjeeling · Mall Road", 27.0410, 88.2640, 15.2)
    World.VICE_COAST -> WorldPreviewPlace("Panaji · Waterfront", 15.4980, 73.8270, 15.2)
    World.LIBERTY -> WorldPreviewPlace("Kolkata · Park Street", 22.5510, 88.3520, 15.8)
    World.CTOS -> WorldPreviewPlace("Hyderabad · HITEC City", 17.4480, 78.3780, 15.8)
    World.HYRULE -> WorldPreviewPlace("Shillong · Ward's Lake", 25.5740, 91.8820, 14.6)
    World.SKYRIM -> WorldPreviewPlace("Manali · Old Town", 32.2430, 77.1890, 14.8)
    World.LANDS_BETWEEN -> WorldPreviewPlace("Hampi · Ruins", 15.3360, 76.4610, 15.0)
    World.HORIZON -> WorldPreviewPlace("Ahmedabad · SG Highway", 23.0340, 72.5070, 15.5)
    World.ASSASSIN -> WorldPreviewPlace("Varanasi · Ghats", 25.3060, 83.0100, 15.6)
    World.FAR_CRY -> WorldPreviewPlace("Munnar · Tea Hills", 10.0890, 77.0590, 14.5)
    World.GHOST -> WorldPreviewPlace("Gangtok · MG Marg", 27.3310, 88.6140, 15.0)
    World.BRIDGES -> WorldPreviewPlace("Pamban · Sea Bridge", 9.2820, 79.2030, 14.3)
    World.HORIZON_WILDS -> WorldPreviewPlace("Kaziranga · Forest Road", 26.5800, 93.1700, 13.5)
    World.SPIDER -> WorldPreviewPlace("Mumbai · Lower Parel", 18.9990, 72.8300, 15.7)
    World.HOGWARTS -> WorldPreviewPlace("Shimla · The Ridge", 31.1048, 77.1734, 15.5)
    World.STARFIELD -> WorldPreviewPlace("Leh · Old Town", 34.1640, 77.5840, 14.8)
    World.KINGDOM -> WorldPreviewPlace("Udaipur · City Palace", 24.5760, 73.6850, 15.5)
    World.SAINTS -> WorldPreviewPlace("Delhi · Connaught Place", 28.6310, 77.2200, 15.8)
    World.JUST_CAUSE -> WorldPreviewPlace("Kochi · Fort Kochi", 9.9660, 76.2420, 15.2)
    World.MAFIA -> WorldPreviewPlace("Kolkata · Dalhousie", 22.5720, 88.3480, 15.8)
    World.DIVISION -> WorldPreviewPlace("Delhi · Connaught Place", 28.631, 77.22, 15.8)
    World.SEA_CHART -> WorldPreviewPlace("Kochi · Fort Kochi", 9.966, 76.242, 15.2)
    World.DOS_ATLAS -> WorldPreviewPlace("Kolkata · Esplanade", 22.566, 88.351, 15.6)
    World.AINCRAD -> WorldPreviewPlace("Udaipur · City Palace", 24.576, 73.685, 15.5)
    World.COUNTER_STRIKE -> WorldPreviewPlace("Jaipur · Old City", 26.926, 75.828, 16.0)
    World.DEFCON -> WorldPreviewPlace("Mumbai · Marine Drive", 18.944, 72.824, 14.6)
    World.FIELD_OPS -> WorldPreviewPlace("Hyderabad · HITEC City", 17.448, 78.378, 15.8)
    World.RETRO_QUEST -> WorldPreviewPlace("Hampi · Ruins", 15.336, 76.461, 15.0)
    World.NIER -> WorldPreviewPlace("Tokyo · Station",35.6812,139.7671,16.0)
    World.STALKER -> WorldPreviewPlace("Dehradun · Clock Tower", 30.3258, 78.0410, 15.6)
    World.HOME_CONSOLE -> WorldPreviewPlace("Panaji · Waterfront", 15.498, 73.827, 15.2)
}

@DrawableRes
fun World.previewImage(): Int = when (this) {
    World.LOS_SANTOS -> R.drawable.preview_los_santos
    World.OLD_WEST -> R.drawable.preview_old_west
    World.GREEN_TERMINAL -> R.drawable.preview_green_terminal
    World.PIXEL -> R.drawable.preview_pixel
    World.NIGHT_CITY -> R.drawable.preview_night_city
    World.SAN_ANDREAS -> R.drawable.preview_san_andreas
    World.HEAT -> R.drawable.preview_heat
    World.RIVIA -> R.drawable.preview_rivia
    World.VICE_COAST -> R.drawable.preview_vice_coast
    World.LIBERTY -> R.drawable.preview_liberty
    World.CTOS -> R.drawable.preview_ctos
    World.HYRULE -> R.drawable.preview_hyrule
    World.SKYRIM -> R.drawable.preview_skyrim
    World.LANDS_BETWEEN -> R.drawable.preview_lands_between
    World.HORIZON -> R.drawable.preview_horizon
    World.ASSASSIN -> R.drawable.preview_assassin
    World.FAR_CRY -> R.drawable.preview_far_cry
    World.GHOST -> R.drawable.preview_ghost
    World.BRIDGES -> R.drawable.preview_bridges
    World.HORIZON_WILDS -> R.drawable.preview_horizon_wilds
    World.SPIDER -> R.drawable.preview_spider
    World.HOGWARTS -> R.drawable.preview_hogwarts
    World.STARFIELD -> R.drawable.preview_starfield
    World.KINGDOM -> R.drawable.preview_kingdom
    World.SAINTS -> R.drawable.preview_saints
    World.JUST_CAUSE -> R.drawable.preview_just_cause
    World.MAFIA -> R.drawable.preview_mafia
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
