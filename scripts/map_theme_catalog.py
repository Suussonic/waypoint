"""Waypoint-owned map materials and symbol grammar for each world.

The colours describe the reviewed game-map references; all land, water, roads,
buildings and labels are drawn from live OpenMapTiles geography using layers
authored by this project. The material and marker families are original vector art.
"""

FIELDS = (
    "ground", "land", "park", "water", "building", "road", "casing",
    "label", "halo", "outline", "font", "material", "marker", "icon_profile",
)

# name|ground|land|park|water|building|road|casing|label|halo|outline|font|material|marker|icon_profile
_ROWS = """
aincrad|#cebc92|#decca4|#9eaa7b|#81a8b0|#b3a78b|#f7eacb|#8c7956|#403c31|#e7d7b4|#f7eacb|Waypoint Classic|vellum|fantasy|layered_orbit
assassin|#b49b6e|#c8b58b|#8d9a63|#426e82|#a49477|#e7d6ad|#857452|#fff1d5|#645840|#e7d6ad|Waypoint Classic|sand-ripple|engraved|animus_sigil
bridges|#182b37|#283c47|#253e3e|#101f32|#344953|#86aab1|#172b37|#b9e8e5|#172c3b|#86aab1|Waypoint Technical|chiral-grid|hologram|chiral_node
counter_strike|#302f2a|#3c3c33|#424a39|#202b31|#65635b|#d6d2bd|#23241e|#f0ebd7|#302f2a|#d6d2bd|Waypoint Stratum|tactical-grid|tactical|bunker_crosshair
ctos|#e5e9dd|#e1e8dc|#a9d4a8|#83cbe3|#c8ced0|#ffffff|#bec8c3|#263b43|#f1f5ef|#ffffff|Waypoint Classic|wayfinder-dots|modern|wayfinder_dots
defcon|#030815|#071127|#0a1c29|#01040c|#112449|#5d9bd4|#040a19|#c6ddff|#030815|#5d9bd4|Waypoint Terminal|vector-graticule|radar|vector_sweep
division|#182c2e|#1f3435|#283e36|#0c2027|#355152|#819a98|#112325|#e3e9df|#182c2e|#819a98|Waypoint Borda|dark-tactical|hologram|shepherd_grid
dos_atlas|#15100d|#1d170e|#272414|#090e11|#4c3d28|#b38d52|#15100d|#ffdf9b|#15100d|#b38d52|Waypoint Terminal|amber-scan|terminal|amber_panel
far_cry|#6e7a60|#90987a|#46694a|#496e79|#818777|#ddd9bc|#505f48|#f2efde|#364c3e|#ddd9bc|Waypoint Condensed|county-topo|field|field_compass
field_ops|#31382f|#3a4235|#3b5137|#233238|#535d4e|#bbc2a8|#252e25|#ecefdc|#31382f|#bbc2a8|Waypoint Condensed|field-grid|tactical|field_grid
ghost|#c9ccc2|#d8d9ce|#9ba69b|#a7b4b1|#aeb4a8|#eeeade|#777f76|#242c27|#e1e2d7|#eeeade|Waypoint Classic|rice-paper|engraved|sumi_brush
green_terminal|#071109|#0a180d|#0f2211|#030904|#102815|#427d35|#102a12|#9af477|#071109|#427d35|Waypoint Condensed|phosphor-scan|terminal|phosphor_grid
heat|#555946|#555946|#3a4932|#234b83|#6a6a5a|#d2d2b6|#383c33|#e7cceb|#20172b|#d2d2b6|Waypoint Condensed|racing-hatch|racing|race_gate
hogwarts|#77765f|#929378|#596c4b|#526d77|#72765e|#c4bea0|#555b45|#e9dfc5|#424939|#c4bea0|Waypoint Atlas|star-vellum|ornate|crest_star
home_console|#e8edf0|#e2e9eb|#b4d5b3|#9ccfe1|#c5cfd7|#ffffff|#b3c4cd|#294b5b|#f3f7f7|#ffffff|Waypoint Classic|soft-console|soft|soft_tile
horizon|#8a966a|#a9ae82|#5c8058|#5095a8|#a3a18a|#f5f0dd|#747852|#f6f1df|#525d3f|#f5f0dd|Waypoint Classic|motorsport-map|racing|rally_checkpoint
horizon_wilds|#5b6251|#5b6251|#354a33|#516d71|#707365|#c6b88c|#30362c|#f3e8cd|#55452f|#c6b88c|Waypoint Classic|oxidized-contour|organic|machine_circuit
hyrule|#4b4932|#4b4932|#424a32|#112a3d|#5f5b3b|#a69c64|#393b2c|#a4dafa|#142330|#a69c64|Waypoint Classic|slate-rosette|slate|slate_eye
just_cause|#143748|#1e4653|#235c5d|#0b2539|#305360|#6fa4ad|#183643|#c4edef|#193745|#6fa4ad|Waypoint Technical|coastal-grid|tactical|coastal_drop
kingdom|#b2a37d|#c4b38a|#76805a|#8caaa5|#a39472|#e7d4aa|#81704e|#3c392b|#d9c9a4|#e7d4aa|Waypoint Atlas|vellum-engraving|engraved|heraldic_seal
lands_between|#b1a789|#b9ac89|#798568|#798b89|#817963|#e0d1aa|#776e54|#eee2c5|#655b43|#e0d1aa|Waypoint Atlas|antique-contour|fantasy|golden_grace
liberty|#000000|#292929|#222222|#050505|#242424|#e5e5e5|#080808|#eeeeee|#050505|#858585|Waypoint Condensed|concrete-blocks|street|metro_roundel
los_santos|#202020|#252525|#30362f|#182126|#383838|#898989|#171717|#e1e1da|#262626|#898989|Waypoint Condensed|street-atlas|street|blip_arc
mafia|#c1c1a4|#d0c9aa|#8ca66b|#83b1b1|#aaa99a|#e8d174|#8e8e74|#3e453c|#dbd5b9|#e8d174|Waypoint Atlas|printed-atlas|engraved|printed_roundel
nier|#CCC8B0|#D5D1BB|#B9BCA4|#9D9F94|#B1AD99|#EAE5CF|#716F60|#35362E|#E5E1CD|#EAE5CF|Waypoint Classic|machine-paper|machine|machine_bracket
night_city|#0c1019|#121d28|#152e32|#070e19|#8b2029|#286b78|#092934|#8fd6dd|#0c1019|#286b78|Waypoint Technical|neon-circuit|neon|neon_circuit
old_west|#dac8a2|#dfcba5|#b9ae89|#a9a58b|#d0bd97|#d9c69e|#413b2e|#3b382d|#dfcba5|#d9c69e|Waypoint Western|trail-paper|western|trail_stamp
pixel|#566943|#6a8151|#3f6743|#3d718b|#777d6c|#c7c4a5|#43513b|#faf4da|#47543b|#c7c4a5|Waypoint Pixel|pixel-biome|pixel|voxel_pin
retro_quest|#88a95c|#a4bf73|#598154|#437ea5|#a2856f|#e5ca89|#786240|#f9f0d4|#4d6239|#e5ca89|Waypoint Retro|16bit-atlas|pixel|quest_pixel
rivia|#b6af8d|#c8bc97|#727f54|#607d7c|#93886b|#e4d6ae|#716548|#33372c|#c8bc97|#e4d6ae|Waypoint Atlas|vellum-contour|fantasy|medallion_ink
saints|#1b1c22|#292932|#2d3740|#3e5962|#363740|#7c8490|#161c27|#e0ddeb|#23232e|#7c8490|Waypoint Classic|violet-grid|street|satellite_target
san_andreas|#c2c5ad|#ceccb7|#7e9c64|#789dba|#a4a48e|#f4f0d8|#737864|#252d27|#d5d2be|#f4f0d8|Waypoint Condensed|county-road|street|street_burst
sea_chart|#d8cbab|#e2d4b1|#b2b18a|#8eaaa9|#aca080|#f2e6c7|#726849|#393327|#e2d4b1|#f2e6c7|Waypoint Atlas|nautical-paper|nautical|nautical_rose
skyrim|#859395|#9aa9a8|#657e7b|#526e7a|#647577|#bdc4be|#626e6d|#f5f6ee|#465354|#bdc4be|Waypoint Futura|northern-relief|frost|north_compass
spider|#102a38|#153948|#164851|#0b2031|#1b6173|#4290a4|#103342|#bcecf4|#102f3c|#4290a4|Waypoint Technical|manhattan-grid|diamond|landmark_diamond
stalker|#3a3f31|#484b39|#273a29|#283435|#66695d|#b6b39a|#252b21|#e4e5d3|#262c22|#b6b39a|Waypoint Condensed|pda-scan|pda|pda_bracket
starfield|#1b304b|#233e58|#28485d|#12273f|#36546a|#7196af|#1b324a|#dce9ed|#20354b|#7196af|Waypoint Technical|survey-grid|survey|survey_reticle
vice_coast|#94909d|#a6a5a4|#3d8960|#6b6d84|#b4b4b0|#f6f1dd|#575e59|#fff4ef|#525465|#f6f1dd|Waypoint Classic|coastal-neon|coastal|sunset_orbit
"""

THEMES = {}
for _line in _ROWS.strip().splitlines():
    _parts = _line.split("|")
    _name, *_values = _parts
    _theme = dict(zip(FIELDS, _values))
    THEMES[_name] = _theme

assert len(THEMES) == 38, f"Expected 38 authored themes, found {len(THEMES)}"
assert len({theme["icon_profile"] for theme in THEMES.values()}) == 38, "Every world must select a distinct POI icon grammar"
assert len({theme["material"] for theme in THEMES.values()}) == 38, "Every world must select its own map material"
assert all(theme["material"] and theme["marker"] for theme in THEMES.values()), "Every world needs a material and marker family"
