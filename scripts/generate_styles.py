"""Build game HUD map palettes from OpenFreeMap Liberty (OSM/OpenMapTiles)."""
import json, copy, hashlib
from cartography_profiles import PROFILES
from pathlib import Path
import urllib.request
source=Path('.tooling/liberty.json')
if not source.exists():
 source.parent.mkdir(parents=True,exist_ok=True)
 with urllib.request.urlopen('https://tiles.openfreemap.org/styles/liberty',timeout=30) as response:
  source.write_bytes(response.read())
# Review a new source deliberately; upstream changes must not silently rewrite themes.
expected_source_sha256 = "408245464e76791801a64393c403433d09e8110eb9516040f8dc2b62584eee2a"
if hashlib.sha256(source.read_bytes()).hexdigest() != expected_source_sha256:
 raise ValueError("OpenFreeMap Liberty changed. Review the new base style before updating its checksum.")
base=json.loads(source.read_text())
palettes={
'los_santos':dict(bg='#202020',land='#252525',park='#30362f',water='#182126',building='#383838',road='#898989',casing='#171717',label='#e1e1da',halo='#262626',outline='#505050'),
'old_west':dict(bg='#dac8a2',land='#dfcba5',park='#a9ac80',water='#9faeac',building='#c6b18d',road='#eee0be',casing='#665f4b',label='#3b382d',halo='#dfcba5',outline='#756c55'),
'night_city':dict(bg='#0c1019',land='#11121f',park='#152e32',water='#050a14',building='#6f1849',road='#258da3',casing='#092934',label='#8fd6dd',halo='#0c1019',outline='#b32c72'),
'pixel':dict(bg='#566943',land='#6a8151',park='#3f6743',water='#3d718b',building='#777d6c',road='#c7c4a5',casing='#43513b',label='#faf4da',halo='#47543b',outline='#a7ad95')}
palettes.update({
'green_terminal':dict(bg='#071109',land='#0a180d',park='#17371a',water='#030904',building='#1e4922',road='#6bcf55',casing='#102a12',label='#9af477',halo='#071109',outline='#3f8835'),
'san_andreas':dict(bg='#c2c5ad',land='#ceccb7',park='#7e9c64',water='#789dba',building='#a4a48e',road='#f4f0d8',casing='#737864',label='#252d27',halo='#d5d2be',outline='#7f846d'),
'heat':dict(bg='#16152a',land='#1f1c36',park='#263846',water='#0d1428',building='#3c305c',road='#cf77c5',casing='#20172b',label='#e7cceb',halo='#20172b',outline='#655281'),
'rivia':dict(bg='#b6af8d',land='#c8bc97',park='#727f54',water='#607d7c',building='#93886b',road='#e4d6ae',casing='#716548',label='#33372c',halo='#c8bc97',outline='#655d47'),
})
palettes.update({
'vice_coast':dict(bg='#94909d',land='#a6a5a4',park='#3d8960',water='#6b6d84',building='#b4b4b0',road='#f6f1dd',casing='#575e59',label='#fff4ef',halo='#525465',outline='#757b75'),
'liberty':dict(bg='#222527',land='#2b2e30',park='#333c36',water='#121518',building='#42484b',road='#92999c',casing='#222426',label='#dbdfda',halo='#24272a',outline='#646c6c'),
'ctos':dict(bg='#e5e9dd',land='#e1e8dc',park='#a9d4a8',water='#83cbe3',building='#c8ced0',road='#ffffff',casing='#bec8c3',label='#263b43',halo='#f1f5ef',outline='#b5bcbe'),
'hyrule':dict(bg='#121f2a',land='#162b38',park='#1b3d47',water='#0a192b',building='#253e4a',road='#48788f',casing='#0d202e',label='#a4dafa',halo='#142330',outline='#345e70'),
'skyrim':dict(bg='#859395',land='#9aa9a8',park='#657e7b',water='#526e7a',building='#647577',road='#bdc4be',casing='#626e6d',label='#f5f6ee',halo='#465354',outline='#677877'),
'lands_between':dict(bg='#b1a789',land='#b9ac89',park='#798568',water='#798b89',building='#817963',road='#e0d1aa',casing='#776e54',label='#eee2c5',halo='#655b43',outline='#6f654e'),
'horizon':dict(bg='#8a966a',land='#a9ae82',park='#5c8058',water='#5095a8',building='#a3a18a',road='#f5f0dd',casing='#747852',label='#f6f1df',halo='#525d3f',outline='#7c8466')
})
terrain_worlds={'rivia','hyrule','skyrim','lands_between','horizon'}
flat_worlds={'old_west','green_terminal','rivia','vice_coast','liberty','ctos','hyrule','skyrim','lands_between','horizon'}
palettes.update({'assassin': {'bg': '#b49b6e', 'land': '#c8b58b', 'park': '#8d9a63', 'water': '#426e82', 'building': '#a49477', 'road': '#e7d6ad', 'casing': '#857452', 'label': '#fff1d5', 'halo': '#645840', 'outline': '#8d805f'}, 'far_cry': {'bg': '#6e7a60', 'land': '#90987a', 'park': '#46694a', 'water': '#496e79', 'building': '#818777', 'road': '#ddd9bc', 'casing': '#505f48', 'label': '#f2efde', 'halo': '#364c3e', 'outline': '#67735f'}, 'ghost': {'bg': '#c9ccc2', 'land': '#d8d9ce', 'park': '#9ba69b', 'water': '#a7b4b1', 'building': '#aeb4a8', 'road': '#eeeade', 'casing': '#777f76', 'label': '#242c27', 'halo': '#e1e2d7', 'outline': '#8b9589'}, 'bridges': {'bg': '#182b37', 'land': '#283c47', 'park': '#253e3e', 'water': '#101f32', 'building': '#344953', 'road': '#86aab1', 'casing': '#172b37', 'label': '#b9e8e5', 'halo': '#172c3b', 'outline': '#45636c'}, 'horizon_wilds': {'bg': '#a28159', 'land': '#b6976d', 'park': '#646c42', 'water': '#557e88', 'building': '#8b775b', 'road': '#e6d5ad', 'casing': '#695439', 'label': '#f3e8cd', 'halo': '#55452f', 'outline': '#88734f'}, 'spider': {'bg': '#102a38', 'land': '#153948', 'park': '#164851', 'water': '#0b2031', 'building': '#1b6173', 'road': '#4290a4', 'casing': '#103342', 'label': '#bcecf4', 'halo': '#102f3c', 'outline': '#287c8c'}, 'hogwarts': {'bg': '#77765f', 'land': '#929378', 'park': '#596c4b', 'water': '#526d77', 'building': '#72765e', 'road': '#c4bea0', 'casing': '#555b45', 'label': '#e9dfc5', 'halo': '#424939', 'outline': '#767f64'}, 'starfield': {'bg': '#1b304b', 'land': '#233e58', 'park': '#28485d', 'water': '#12273f', 'building': '#36546a', 'road': '#7196af', 'casing': '#1b324a', 'label': '#dce9ed', 'halo': '#20354b', 'outline': '#486b83'}, 'kingdom': {'bg': '#b2a37d', 'land': '#c4b38a', 'park': '#76805a', 'water': '#8caaa5', 'building': '#a39472', 'road': '#e7d4aa', 'casing': '#81704e', 'label': '#3c392b', 'halo': '#d9c9a4', 'outline': '#8e805b'}, 'saints': {'bg': '#1b1c22', 'land': '#292932', 'park': '#2d3740', 'water': '#3e5962', 'building': '#363740', 'road': '#7c8490', 'casing': '#161c27', 'label': '#e0ddeb', 'halo': '#23232e', 'outline': '#505565'}, 'just_cause': {'bg': '#143748', 'land': '#1e4653', 'park': '#235c5d', 'water': '#0b2539', 'building': '#305360', 'road': '#6fa4ad', 'casing': '#183643', 'label': '#c4edef', 'halo': '#193745', 'outline': '#3e7580'}, 'mafia': {'bg': '#c1c1a4', 'land': '#d0c9aa', 'park': '#8ca66b', 'water': '#83b1b1', 'building': '#aaa99a', 'road': '#e8d174', 'casing': '#8e8e74', 'label': '#3e453c', 'halo': '#dbd5b9', 'outline': '#8d9585'}})
terrain_worlds.update({'horizon_wilds', 'kingdom', 'bridges', 'assassin', 'far_cry', 'starfield', 'ghost', 'hogwarts', 'just_cause'})
flat_worlds.update({'horizon_wilds', 'kingdom', 'bridges', 'assassin', 'mafia', 'starfield', 'ghost', 'hogwarts', 'just_cause'})
# Official owner-published worlds from issue #7; see docs/visual-sourcebook.md.
palettes.update({'division': {'bg': '#182c2e', 'land': '#1f3435', 'park': '#283e36', 'water': '#0c2027', 'building': '#355152', 'road': '#819a98', 'casing': '#112325', 'label': '#e3e9df', 'halo': '#182c2e', 'outline': '#517575'}, 'sea_chart': {'bg': '#d8cbab', 'land': '#e2d4b1', 'park': '#b2b18a', 'water': '#8eaaa9', 'building': '#aca080', 'road': '#f2e6c7', 'casing': '#726849', 'label': '#393327', 'halo': '#e2d4b1', 'outline': '#938566'}, 'dos_atlas': {'bg': '#15100d', 'land': '#1d170e', 'park': '#272414', 'water': '#090e11', 'building': '#4c3d28', 'road': '#b38d52', 'casing': '#15100d', 'label': '#ffdf9b', 'halo': '#15100d', 'outline': '#85673c'}, 'aincrad': {'bg': '#cebc92', 'land': '#decca4', 'park': '#9eaa7b', 'water': '#81a8b0', 'building': '#b3a78b', 'road': '#f7eacb', 'casing': '#8c7956', 'label': '#403c31', 'halo': '#e7d7b4', 'outline': '#9c906f'}, 'counter_strike': {'bg': '#302f2a', 'land': '#3c3c33', 'park': '#424a39', 'water': '#202b31', 'building': '#65635b', 'road': '#d6d2bd', 'casing': '#23241e', 'label': '#f0ebd7', 'halo': '#302f2a', 'outline': '#939084'}, 'defcon': {'bg': '#030815', 'land': '#071127', 'park': '#0a1c29', 'water': '#01040c', 'building': '#112449', 'road': '#5d9bd4', 'casing': '#040a19', 'label': '#c6ddff', 'halo': '#030815', 'outline': '#24476f'}, 'field_ops': {'bg': '#31382f', 'land': '#3a4235', 'park': '#3b5137', 'water': '#233238', 'building': '#535d4e', 'road': '#bbc2a8', 'casing': '#252e25', 'label': '#ecefdc', 'halo': '#31382f', 'outline': '#77816b'}, 'retro_quest': {'bg': '#88a95c', 'land': '#a4bf73', 'park': '#598154', 'water': '#437ea5', 'building': '#a2856f', 'road': '#e5ca89', 'casing': '#786240', 'label': '#f9f0d4', 'halo': '#4d6239', 'outline': '#705541'}, 'home_console': {'bg': '#e8edf0', 'land': '#e2e9eb', 'park': '#b4d5b3', 'water': '#9ccfe1', 'building': '#c5cfd7', 'road': '#ffffff', 'casing': '#b3c4cd', 'label': '#294b5b', 'halo': '#f3f7f7', 'outline': '#a8bbc6'}})
# Issue #8: PDA field map. Real geography; no fictional anomaly or hazard overlays.
palettes['nier']=dict(bg='#CCC8B0',land='#D5D1BB',park='#B9BCA4',water='#9D9F94',building='#B1AD99',road='#EAE5CF',casing='#716F60',label='#35362E',halo='#E5E1CD',outline='#84816E')
palettes['stalker']=dict(bg='#3a3f31',land='#484b39',park='#273a29',water='#283435',building='#66695d',road='#b6b39a',casing='#252b21',label='#e4e5d3',halo='#262c22',outline='#818574')
# Corrections from verified map screens, rather than neon gameplay associations.
palettes['night_city'].update(building='#8b2029',outline='#c94045',land='#121d28',road='#286b78',water='#070e19')
palettes['old_west'].update(road='#d9c69e',casing='#413b2e',building='#d0bd97',park='#b9ae89',water='#a9a58b')
palettes['green_terminal'].update(road='#427d35',building='#102815',park='#0f2211')
palettes['hyrule'].update(bg='#4b4932',land='#4b4932',park='#424a32',water='#112a3d',road='#a69c64',casing='#393b2c',building='#5f5b3b')
palettes['heat'].update(bg='#555946',land='#555946',park='#3a4932',water='#234b83',road='#d2d2b6',building='#6a6a5a',casing='#383c33')
palettes['horizon_wilds'].update(bg='#5b6251',land='#5b6251',park='#354a33',water='#516d71',road='#c6b88c',building='#707365',casing='#30362c')
terrain_worlds.add('heat')
flat_worlds={n for n,p in PROFILES.items() if p['building'] not in ('extrude','pixel')}
def scale_width(value,scale):
 if isinstance(value,(int,float)):return value*scale
 if isinstance(value,dict):
  v=copy.deepcopy(value)
  if 'stops' in v:v['stops']=[[z,n*scale] for z,n in v['stops']]
  return v
 if isinstance(value,list) and value and value[0] in ('interpolate','interpolate-hcl','interpolate-lab','step'):
  # Camera expressions must stay top-level; multiplying the whole expression
  # makes MapLibre discard the road layer. Scale only its output stops.
  v=copy.deepcopy(value)
  outputs=range(4,len(v),2) if value[0]!='step' else [2]+list(range(4,len(v),2))
  for i in outputs:v[i]=scale_width(v[i],scale)
  return v
 return ['*',value,scale]
map_fonts={name:'Waypoint Classic' for name in palettes}
for name in ('night_city','bridges','spider','starfield','just_cause'):map_fonts[name]='Waypoint Technical'
for name in ('rivia','lands_between','kingdom','hogwarts','mafia'):map_fonts[name]='Waypoint Atlas'
map_fonts['old_west']='Waypoint Western'
for name in ('los_santos','green_terminal','skyrim','san_andreas','liberty','heat','far_cry'):map_fonts[name]='Waypoint Condensed'
map_fonts['pixel']='Waypoint Pixel'
map_fonts.update({'division': 'Waypoint Borda', 'sea_chart': 'Waypoint Atlas', 'dos_atlas': 'Waypoint Terminal', 'aincrad': 'Waypoint Classic', 'counter_strike': 'Waypoint Stratum', 'defcon': 'Waypoint Terminal', 'field_ops': 'Waypoint Condensed', 'retro_quest': 'Waypoint Retro', 'home_console': 'Waypoint Classic'})
map_fonts['stalker']='Waypoint Condensed'
map_fonts['skyrim']='Waypoint Futura'
for name,c in palettes.items():
 profile=PROFILES[name]
 s=copy.deepcopy(base); s['name']='Waypoint '+name; s['layers']=[l for l in s['layers'] if l['type'] not in ('raster','fill-extrusion') and 'pattern' not in l['id']]
 for l in s['layers']:
  p=l.setdefault('paint',{}); source=l.get('source-layer',''); id=l['id']; typ=l['type']
  if typ=='background':
   p['background-color']=c['bg']
   if name=='pixel':p['background-pattern']='pixel-grass'
  if typ=='fill':
   color=c['land']
   if source in ('park','landcover') or any(k in id for k in ['pitch','cemetery']):color=c['park']
   if source=='water':color=c['water']
   if source=='building':
    color=c['building'];p['fill-outline-color']=c['outline']
    if name=='old_west':p['fill-opacity']=0.55
    if name in ('skyrim','lands_between'):p['fill-opacity']=0.45
   p['fill-color']=color;p.pop('fill-pattern',None)
   if source in ('park','landcover'):p['fill-opacity']=profile['park']
   if source=='building':
    if profile['building']=='hidden':l.setdefault('layout',{})['visibility']='none'
    elif profile['building']=='outline':p['fill-opacity']=0.18
    elif profile['building']=='ink':p['fill-opacity']=0.42
    else:p['fill-opacity']=0.8
   if name=='pixel' and source in ('landcover','landuse','park'):p['fill-pattern']='pixel-grass';p['fill-opacity']=0.8
   if name=='pixel' and source=='water':p['fill-pattern']='pixel-water'
   if name=='pixel' and source=='building':p['fill-pattern']='pixel-stone'

  if typ=='line':
   p['line-color']=c['water'] if source=='waterway' else (c['casing'] if 'casing' in id else c['road'])
   if 'rail' in id:p['line-color']=c['outline']
   if source=='transportation':
    factor=profile['case'] if 'casing' in id else profile['road']
    if 'line-width' in p:p['line-width']=scale_width(p['line-width'],factor)
    if factor==0:l.setdefault('layout',{})['visibility']='none'
    if name in ('old_west','ghost','kingdom','rivia') and any(t in id for t in ('path','track')):p['line-dasharray']=[2,2]

  if typ=='symbol':
   if 'text-field' in l.get('layout',{}):l['layout']['text-font']=[map_fonts[name]]
   p['text-color']=c['label'];p['text-halo-color']=c['halo'];p['text-halo-width']=1.3
   if source=='poi':l.setdefault('layout',{})['visibility']='none'
   if source=='transportation_name':
    l.setdefault('layout',{})['text-size']=profile['labels']
    l['layout']['text-font']=[map_fonts[name]]
    l['layout']['text-letter-spacing']=0.04 if name in ('old_west','ghost','rivia') else 0
    l['minzoom']=14
    if name in ('old_west','liberty','green_terminal'):l['layout']['text-transform']='uppercase'
    l['layout']['symbol-spacing']=300
   if id in ('road_one_way_arrow','road_one_way_arrow_opposite'):
    l['minzoom']=17
  if typ=='fill-extrusion':
   p['fill-extrusion-color']=c['building']
 # Real terrain relief for the open-world atlas styles, below roads and labels.
 if name in terrain_worlds:
  s['sources']['terrain']={'type':'raster-dem','tiles':['https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png'],'encoding':'terrarium','tileSize':256,'maxzoom':15,'attribution':'Terrain © Mapzen / data providers'}
  terrain_index=next((i for i,l in enumerate(s['layers']) if l.get('source-layer') in ('transportation','building')),len(s['layers']))
  s['layers'].insert(terrain_index,{'id':'world-terrain-relief','type':'hillshade','source':'terrain','paint':{'hillshade-exaggeration':profile['relief'],'hillshade-shadow-color':c['casing'],'hillshade-highlight-color':c['road'],'hillshade-accent-color':c['outline']}})
 if profile['paper']:
  before_roads=next((i for i,l in enumerate(s['layers']) if l.get('source-layer')=='transportation'),len(s['layers']))
  s['layers'].insert(before_roads,{'id':'atlas-paper-fibre','type':'background','paint':{'background-pattern':'paper-fibre','background-opacity':0.4}})
 if profile['building'] in ('ink','outline'):
  idx=next((i for i,l in enumerate(s['layers']) if l['type']=='symbol'),len(s['layers']))
  s['layers'].insert(idx,{'id':'atlas-building-ink','type':'line','source':'openmaptiles','source-layer':'building','minzoom':15,'paint':{'line-color':c['outline'],'line-width':0.8 if profile['building']=='ink' else 0.5,'line-opacity':0.75}})
 # Dim short buildings with native vector height; retain 2D ink in the western palette.
 before=next((i for i,l in enumerate(s['layers']) if l['type']=='symbol'),len(s['layers']))
 if name not in flat_worlds:
  s['layers'].insert(before,{'id':'waypoint-buildings','type':'fill-extrusion','source':'openmaptiles','source-layer':'building','minzoom':14.5,
   'paint':{'fill-extrusion-color':c['building'],'fill-extrusion-height':['interpolate',['linear'],['zoom'],14.5,0,16,['coalesce',['get','render_height'],8]],'fill-extrusion-base':['coalesce',['get','render_min_height'],0],'fill-extrusion-opacity':0.9,**({'fill-extrusion-pattern':'pixel-stone'} if name=='pixel' else {})}})
 Path(f'app/src/main/assets/styles/{name}.json').write_text(json.dumps(s,separators=(',',':')))
