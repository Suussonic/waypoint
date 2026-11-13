package com.waypoint.app

import android.content.Context
import android.graphics.*

internal fun mapMaterial(context:Context,world:World,night:Boolean):Bitmap? = runCatching {
    context.assets.open("map-materials/${world.file}_${if(night)"night" else "day"}.png")
        .use(BitmapFactory::decodeStream)
}.getOrNull()

/**
 * Per-world texture kit: every PNG in assets/map-textures/<world>/ becomes a style image named after
 * its file, so a style can reference it from fill-pattern or line-pattern. Worlds without a kit return
 * an empty map. Tiles are authored at 2x, so they carry a fixed density instead of the phone's.
 */
internal fun mapTextureKit(context:Context,world:World):Map<String,Bitmap> = runCatching {
    val dir="map-textures/${world.file}"
    context.assets.list(dir).orEmpty().filter { it.endsWith(".png") }.associate { name ->
        name.removeSuffix(".png") to context.assets.open("$dir/$name").use(BitmapFactory::decodeStream).also {
            it.density=android.util.DisplayMetrics.DENSITY_DEFAULT*2
        }
    }
}.getOrDefault(emptyMap())

/** Independent, geographic rendering rules; all points and building heights stay real. */
data class MapArt(val tilt:Double,val zoom:Double,val routeWidth:Float,val poiScale:Float,val marker:MarkerArt,val player:PlayerArt,val lightLabels:Boolean)
enum class MarkerArt { SILHOUETTE, INK, TECH, PIXEL, PHOSPHOR, PASTEL, SHEIKAH, SILVER, GOLD, RACING, DIAMOND, SURVEY }
enum class PlayerArt { ARROW, TEARDROP, CHEVRON, PIXEL, POINTER, RING, CROSSHAIR, COMPASS, STAR, BEACON, SHIELD, ROUNDEL, DIAMOND, PDA, WEDGE, SQUARE }
enum class RouteGrammar { SOLID, DASHED, DOTS, LONG_DASH, TICKED }

/** Each world gives the same provider route geometry its own legible line grammar. */
fun World.routeGrammar(mode:TravelMode?=null,navigating:Boolean=false)=when(this) {
    World.NIGHT_CITY->if(mode==TravelMode.WALK)RouteGrammar.DOTS else RouteGrammar.SOLID
    World.SAINTS->if(navigating)RouteGrammar.DOTS else RouteGrammar.SOLID
    World.OLD_WEST,World.RIVIA,World.ASSASSIN,World.GHOST,World.HYRULE,
    World.LANDS_BETWEEN,World.KINGDOM,World.HOGWARTS,World.SEA_CHART->RouteGrammar.DASHED
    World.DEFCON,World.STARFIELD->RouteGrammar.DOTS
    World.BRIDGES,World.DIVISION,World.CTOS,World.STALKER->RouteGrammar.TICKED
    World.FAR_CRY,World.HORIZON_WILDS,World.JUST_CAUSE,World.FIELD_OPS->RouteGrammar.LONG_DASH
    else->RouteGrammar.SOLID
}

fun World.routeLineDash(mode:TravelMode?=null,navigating:Boolean=false)=when(routeGrammar(mode,navigating)) {
    RouteGrammar.SOLID->arrayOf(1f,0f)
    RouteGrammar.DASHED->arrayOf(2.1f,1.15f)
    RouteGrammar.DOTS->arrayOf(.35f,1.35f)
    RouteGrammar.LONG_DASH->arrayOf(3.2f,1.25f)
    RouteGrammar.TICKED->arrayOf(1.65f,.48f,.32f,.48f)
}

/** Contrast stroke stays visible over this world's day and night map materials. */
fun World.routeCasingColor(night:Boolean):String {
    if(night)return "#071018"
    val r=ground.red;val g=ground.green;val b=ground.blue
    val luminance=.2126f*r+.7152f*g+.0722f*b
    return if(luminance<.16f)"#F1F4EE" else "#17211F"
}

fun World.mapArt()=when(this) {
    World.LOS_SANTOS->MapArt(0.0,16.2,5f,.55f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.OLD_WEST->MapArt(0.0,16.0,3.8f,.55f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.NIGHT_CITY->MapArt(45.0,16.2,3.8f,.55f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.PIXEL->MapArt(42.0,17.0,5f,.6f,MarkerArt.PIXEL,PlayerArt.PIXEL,true)
    World.GREEN_TERMINAL->MapArt(0.0,16.1,3f,.6f,MarkerArt.PHOSPHOR,PlayerArt.CROSSHAIR,true)
    World.SAN_ANDREAS->MapArt(0.0,16.0,5f,.7f,MarkerArt.SILHOUETTE,PlayerArt.WEDGE,false)
    World.HEAT->MapArt(0.0,15.8,4.5f,.65f,MarkerArt.RACING,PlayerArt.ARROW,true)
    World.RIVIA->MapArt(0.0,15.6,3f,.58f,MarkerArt.GOLD,PlayerArt.COMPASS,false)
    World.VICE_COAST->MapArt(0.0,16.0,5f,.7f,MarkerArt.SILHOUETTE,PlayerArt.ROUNDEL,true)
    World.LIBERTY->MapArt(0.0,16.1,4.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.SQUARE,true)
    World.CTOS->MapArt(0.0,16.0,4f,.62f,MarkerArt.PASTEL,PlayerArt.RING,false)
    World.HYRULE->MapArt(0.0,15.7,2.5f,.55f,MarkerArt.SHEIKAH,PlayerArt.COMPASS,true)
    World.SKYRIM->MapArt(35.0,15.5,2f,.62f,MarkerArt.SILVER,PlayerArt.TEARDROP,true)
    World.LANDS_BETWEEN->MapArt(0.0,15.6,2.5f,.65f,MarkerArt.GOLD,PlayerArt.STAR,false)
    World.HORIZON->MapArt(0.0,15.9,4.5f,.65f,MarkerArt.RACING,PlayerArt.ARROW,true)
    World.ASSASSIN->MapArt(0.0,15.6,2.5f,.55f,MarkerArt.GOLD,PlayerArt.WEDGE,true)
    World.FAR_CRY->MapArt(30.0,15.7,3.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.SHIELD,true)
    World.GHOST->MapArt(0.0,15.6,2.5f,.55f,MarkerArt.DIAMOND,PlayerArt.STAR,false)
    World.BRIDGES->MapArt(25.0,15.7,2.5f,.5f,MarkerArt.TECH,PlayerArt.BEACON,true)
    World.HORIZON_WILDS->MapArt(0.0,15.6,3f,.55f,MarkerArt.SILVER,PlayerArt.DIAMOND,true)
    World.SPIDER->MapArt(40.0,16.1,4f,.55f,MarkerArt.DIAMOND,PlayerArt.CHEVRON,true)
    World.HOGWARTS->MapArt(25.0,15.5,2.5f,.6f,MarkerArt.GOLD,PlayerArt.STAR,true)
    World.STARFIELD->MapArt(30.0,15.9,2.5f,.5f,MarkerArt.SURVEY,PlayerArt.POINTER,true)
    World.KINGDOM->MapArt(0.0,15.6,3f,.65f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.SAINTS->MapArt(0.0,16.0,4f,.6f,MarkerArt.PASTEL,PlayerArt.ROUNDEL,true)
    World.JUST_CAUSE->MapArt(0.0,15.6,3f,.55f,MarkerArt.DIAMOND,PlayerArt.POINTER,true)
    World.MAFIA->MapArt(0.0,16.0,4f,.6f,MarkerArt.INK,PlayerArt.SQUARE,false)
    World.DIVISION->MapArt(40.0,16.0,4f,.6f,MarkerArt.TECH,PlayerArt.SHIELD,true)
    World.SEA_CHART->MapArt(0.0,15.6,3.5f,.65f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.DOS_ATLAS->MapArt(0.0,16.0,3f,.55f,MarkerArt.PHOSPHOR,PlayerArt.PDA,true)
    World.AINCRAD->MapArt(0.0,15.8,3.5f,.6f,MarkerArt.GOLD,PlayerArt.RING,false)
    World.COUNTER_STRIKE->MapArt(0.0,16.7,4f,.65f,MarkerArt.SILHOUETTE,PlayerArt.SHIELD,true)
    World.DEFCON->MapArt(0.0,15.5,2.5f,.5f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.FIELD_OPS->MapArt(0.0,16.2,3.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.BEACON,true)
    World.RETRO_QUEST->MapArt(0.0,16.0,4f,.6f,MarkerArt.PIXEL,PlayerArt.PIXEL,false)
    World.NIER->MapArt(42.0,16.0,4.5f,.6f,MarkerArt.DIAMOND,PlayerArt.RING,false)
    World.STALKER->MapArt(0.0,15.8,4f,.6f,MarkerArt.PHOSPHOR,PlayerArt.PDA,true)
    World.HOME_CONSOLE->MapArt(25.0,16.0,4f,.6f,MarkerArt.PASTEL,PlayerArt.ROUNDEL,false)
}
/**
 * Street-level camera distances. Locate and navigation both frame the vehicle closely enough to see which
 * road it is on and which side of a junction it has taken, without squinting. Vector tiles are
 * over-zoomed past 14, so these levels cost no extra tile data.
 */
internal const val LOCATE_ZOOM = 18.3
internal const val NAV_ZOOM = 18.5

/**
 * The route is drawn wider as the map zooms in, and always wider than the widest road at that zoom
 * (roads reach about 14 px at zoom 18), so it never disappears under the road it follows.
 */
internal fun routeWidthAtZoom(base: Float, extra: Float): org.maplibre.android.style.expressions.Expression =
    org.maplibre.android.style.expressions.Expression.interpolate(
        org.maplibre.android.style.expressions.Expression.linear(), org.maplibre.android.style.expressions.Expression.zoom(),
        org.maplibre.android.style.expressions.Expression.stop(10, base + extra),
        org.maplibre.android.style.expressions.Expression.stop(14, base * 1.4f + extra),
        org.maplibre.android.style.expressions.Expression.stop(18, base * 2f + 8f + extra),
        org.maplibre.android.style.expressions.Expression.stop(20, base * 2.4f + 12f + extra),
    )

/** Camera padding that puts the followed vehicle about three quarters of the way down the screen while guiding. */
internal fun drivePadding(map:org.maplibre.android.maps.MapLibreMap,navigating:Boolean)=
    doubleArrayOf(0.0,if(navigating)map.height*DRIVE_TOP_PADDING_FRACTION else 0.0,0.0,0.0)
internal const val DRIVE_TOP_PADDING_FRACTION=0.46
internal const val DRIVE_TILT=55.0
internal fun mapCameraTilt(world:World,enabled:Boolean,navigating:Boolean=false,previewTilt:Double?=null):Double =
    previewTilt ?: if(!enabled) 0.0 else if(navigating) DRIVE_TILT else world.mapArt().tilt.takeIf { it>0.0 } ?: 45.0
private fun color(hex:String)=Color.parseColor(hex)
private fun World.markerInk()=when(mapArt().marker){MarkerArt.PHOSPHOR->color("#A4F780");MarkerArt.TECH,MarkerArt.SHEIKAH->color("#7DDDEC");MarkerArt.GOLD->color("#ECD797");else->Color.WHITE}
private object PoiAtlas {
    private var loadedKey:String?=null
    private var loaded:Bitmap?=null

    @Synchronized fun icon(context:Context,kind:String,world:World,night:Boolean):Bitmap? {
        val categories=MapPoiCategories.all(context)
        val index=categories.indexOfFirst { it.id==kind }
        if(index<0)return null
        val assetName="${world.file}${if(night)"_night" else ""}"
        if(loadedKey!=assetName) {
            val decoded=runCatching {context.assets.open("poi/$assetName.png").use(BitmapFactory::decodeStream)}.getOrNull()
            loaded?.takeUnless { it.isRecycled }?.recycle()
            loaded=decoded
            loadedKey=assetName
        }
        val atlas=loaded ?: return null
        if(atlas.width!=MapPoiCategories.atlasWidth() || atlas.height<MapPoiCategories.atlasHeight(categories.size))return null
        val tile=Bitmap.createBitmap(atlas,
            (index%MapPoiCategories.columns())*MapPoiCategories.cellSize(),
            (index/MapPoiCategories.columns())*MapPoiCategories.cellSize(),
            MapPoiCategories.cellSize(),MapPoiCategories.cellSize())
        return Bitmap.createScaledBitmap(tile,64,64,world!=World.PIXEL && world!=World.SAN_ANDREAS)
    }
}

/** GTA IV's Liberty City radar marks, reused for the closest live OSM category. */
private object LibertyBlips {
    private val filesByCategory = mapOf(
        "place" to "centre",
        "restaurant" to "restaurant",
        "fast_food" to "burgershot",
        "ice_cream" to "chicken",
        "cafe" to "bar",
        "pub" to "darts",
        "bar" to "club",
        "lodging" to "safehouse",
        "fuel" to "car",
        "ev_charging" to "car",
        "parking" to "car",
        "grocery" to "deal",
        "convenience" to "deal",
        "bakery" to "chicken",
        "butcher" to "restaurant",
        "shop" to "sprayshop",
        "clothing" to "clothes",
        "mall" to "clothes",
        "pharmacy" to "hospital",
        "hospital" to "hospital",
        "clinic" to "hospital",
        "bank" to "deal",
        "atm" to "deal",
        "school" to "multiplayer",
        "university" to "multiplayer",
        "library" to "lawyer",
        "museum" to "cup",
        "attraction" to "helitour",
        "castle" to "assassin",
        "monument" to "centre",
        "cinema" to "comedy",
        "theatre" to "cabaret",
        "park" to "girlfriend",
        "playground" to "8ball",
        "garden" to "girlfriend",
        "toilets" to "centre",
        "drinking_water" to "centre",
        "information" to "north",
        "police" to "police",
        "fire_station" to "fire",
        "post_office" to "station",
        "place_of_worship" to "lawyer",
        "stadium" to "cup",
        "sports" to "bowling",
        "train_station" to "station",
        "bus_stop" to "taxi",
        "ferry_terminal" to "boattour",
        "car_repair" to "carwash",
        "office" to "lawyer",
    )
    private val decoded = HashMap<String, Bitmap>()

    @Synchronized fun icon(context: Context, category: String): Bitmap? {
        val file = filesByCategory[category] ?: return null
        decoded[file]?.let { return it }
        return runCatching {
            context.assets.open("poi/liberty_blips/radar_$file.png").use(BitmapFactory::decodeStream)
        }.getOrNull()?.also { decoded[file] = it }
    }
}

/**
 * Size of the position marker (car, bike, arrow) by zoom. The close-in size is unchanged; zoomed out it shrinks
 * so the marker stays proportionate to the roads instead of covering them.
 */
internal fun playerIconSize(base:Float):org.maplibre.android.style.expressions.Expression {
    return org.maplibre.android.style.expressions.Expression.interpolate(
        org.maplibre.android.style.expressions.Expression.linear(),org.maplibre.android.style.expressions.Expression.zoom(),
        org.maplibre.android.style.expressions.Expression.stop(10f,base*.3f),
        org.maplibre.android.style.expressions.Expression.stop(12f,base*.38f),
        org.maplibre.android.style.expressions.Expression.stop(14f,base*.52f),
        org.maplibre.android.style.expressions.Expression.stop(16f,base*.74f),
        org.maplibre.android.style.expressions.Expression.stop(18f,base),
        org.maplibre.android.style.expressions.Expression.stop(22f,base*1.1f))
}
internal fun mapPlayer(world:World):Bitmap {
    val b=Bitmap.createBitmap(96,104,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG);val ink=if(world==World.SAINTS)color("#FFDA45") else world.markerInk()
    fun line(path:Path,color:Int,width:Float=7f) { p.color=Color.BLACK;p.style=Paint.Style.STROKE;p.strokeWidth=width+4f;p.strokeJoin=Paint.Join.ROUND;p.strokeCap=Paint.Cap.ROUND;c.drawPath(path,p);p.color=color;p.strokeWidth=width;c.drawPath(path,p) }
    fun shape(path:Path,fill:Int=ink) { p.style=Paint.Style.FILL;p.color=Color.BLACK;c.drawPath(path,p);p.color=fill;c.drawPath(path,p);p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=5f;p.strokeJoin=Paint.Join.ROUND;c.drawPath(path,p);p.style=Paint.Style.FILL }
    fun polygon(vararg points:Float)=Path().apply { moveTo(points[0],points[1]);for(i in 2 until points.size step 2)lineTo(points[i],points[i+1]);close() }
    when(world.mapArt().player) {
        PlayerArt.ARROW -> shape(polygon(48f,7f,78f,88f,49f,69f,18f,88f))
        PlayerArt.TEARDROP -> { val path=Path().apply{moveTo(48f,7f);cubicTo(32f,29f,13f,48f,18f,69f);cubicTo(22f,91f,39f,99f,48f,99f);cubicTo(57f,99f,74f,91f,78f,69f);cubicTo(83f,48f,64f,29f,48f,7f);close()};shape(path,Color.WHITE);p.color=ink;c.drawCircle(48f,66f,13f,p) }
        PlayerArt.CHEVRON -> { line(Path().apply{moveTo(20f,32f);lineTo(48f,68f);lineTo(76f,32f)},ink,7f);line(Path().apply{moveTo(30f,29f);lineTo(48f,51f);lineTo(66f,29f)},Color.WHITE,4f) }
        PlayerArt.PIXEL -> { p.isAntiAlias=false;p.color=Color.BLACK;c.drawRect(35f,8f,61f,34f,p);c.drawRect(25f,34f,71f,80f,p);c.drawRect(16f,48f,80f,68f,p);p.color=ink;c.drawRect(40f,14f,56f,30f,p);c.drawRect(31f,40f,65f,74f,p);c.drawRect(22f,52f,74f,63f,p) }
        PlayerArt.POINTER -> { shape(polygon(18f,8f,74f,59f,49f,62f,40f,88f));p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=6f;c.drawLine(40f,54f,58f,83f,p);p.color=ink;p.strokeWidth=3f;c.drawLine(40f,54f,58f,83f,p) }
        PlayerArt.RING -> { p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=14f;c.drawCircle(48f,57f,26f,p);p.color=ink;p.strokeWidth=6f;c.drawCircle(48f,57f,26f,p);shape(polygon(48f,5f,36f,33f,60f,33f));p.color=Color.BLACK;c.drawCircle(48f,57f,7f,p) }
        PlayerArt.CROSSHAIR -> { p.color=Color.BLACK;p.strokeWidth=10f;p.strokeCap=Paint.Cap.SQUARE;c.drawCircle(48f,52f,23f,p);p.color=ink;p.strokeWidth=4f;c.drawCircle(48f,52f,18f,p);for(a in listOf(0,90,180,270)){c.save();c.rotate(a.toFloat(),48f,52f);line(Path().apply{moveTo(48f,4f);lineTo(48f,20f);moveTo(48f,84f);lineTo(48f,100f)},ink,4f);c.restore()};p.color=Color.WHITE;c.drawCircle(48f,52f,5f,p) }
        PlayerArt.COMPASS -> { shape(polygon(48f,5f,58f,43f,92f,52f,58f,61f,48f,99f,38f,61f,4f,52f,38f,43f));p.color=Color.WHITE;c.drawCircle(48f,52f,7f,p) }
        PlayerArt.STAR -> { val points=FloatArray(20);for(i in 0 until 10){val angle=(i*Math.PI/5.0-Math.PI/2);val radius=if(i%2==0)42f else 19f;points[i*2]=48f+radius*kotlin.math.cos(angle).toFloat();points[i*2+1]=52f+radius*kotlin.math.sin(angle).toFloat()};shape(Path().apply{moveTo(points[0],points[1]);for(i in 1 until 10)lineTo(points[i*2],points[i*2+1]);close()});p.color=Color.BLACK;c.drawCircle(48f,52f,8f,p) }
        PlayerArt.BEACON -> { p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=10f;c.drawCircle(48f,59f,12f,p);p.color=ink;p.strokeWidth=4f;c.drawCircle(48f,59f,12f,p);for(radius in listOf(27f,39f)){p.color=ink;p.strokeWidth=3f;c.drawArc(48f-radius,59f-radius,48f+radius,59f+radius,210f,120f,false,p);c.drawArc(48f-radius,59f-radius,48f+radius,59f+radius,30f,120f,false,p)};shape(polygon(48f,5f,37f,31f,59f,31f)) }
        PlayerArt.SHIELD -> { shape(polygon(48f,7f,79f,20f,74f,62f,48f,96f,22f,62f,17f,20f));line(Path().apply{moveTo(33f,48f);lineTo(46f,61f);lineTo(65f,39f)},Color.WHITE,5f) }
        PlayerArt.ROUNDEL -> { p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=12f;c.drawCircle(48f,53f,34f,p);p.color=ink;p.strokeWidth=5f;c.drawCircle(48f,53f,34f,p);shape(polygon(48f,10f,38f,45f,58f,45f));p.color=Color.WHITE;c.drawCircle(48f,58f,7f,p) }
        PlayerArt.DIAMOND -> { shape(polygon(48f,5f,87f,52f,48f,99f,9f,52f));line(Path().apply{moveTo(48f,20f);lineTo(48f,81f);moveTo(28f,52f);lineTo(68f,52f)},Color.WHITE,3f) }
        PlayerArt.PDA -> { p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.SQUARE;p.strokeJoin=Paint.Join.MITER;p.color=Color.BLACK;p.strokeWidth=10f;c.drawRect(17f,18f,79f,86f,p);p.color=ink;p.strokeWidth=4f;c.drawRect(17f,18f,79f,86f,p);line(Path().apply{moveTo(48f,32f);lineTo(48f,72f);moveTo(30f,52f);lineTo(66f,52f)},Color.WHITE,4f);p.style=Paint.Style.FILL;p.color=ink;c.drawCircle(48f,52f,5f,p) }
        PlayerArt.WEDGE -> { p.color=Color.BLACK;c.drawCircle(48f,56f,35f,p);p.color=ink;c.drawCircle(48f,56f,27f,p);shape(polygon(48f,7f,74f,86f,48f,68f,22f,86f),Color.WHITE);p.color=Color.BLACK;c.drawCircle(48f,55f,8f,p) }
        PlayerArt.SQUARE -> { p.style=Paint.Style.STROKE;p.color=Color.BLACK;p.strokeWidth=12f;c.drawRect(17f,20f,79f,82f,p);p.color=ink;p.strokeWidth=5f;c.drawRect(17f,20f,79f,82f,p);shape(polygon(48f,28f,68f,66f,48f,55f,28f,66f));p.color=Color.WHITE;c.drawCircle(48f,51f,5f,p) }
    }
    return b
}

/** Night City uses one dedicated directional caret on phone, car display, and in the map key. */
internal fun mapPlayer(context:Context,world:World):Bitmap =
    if(world==World.NIGHT_CITY) runCatching {
        context.assets.open("player-markers/night_city.png").use(BitmapFactory::decodeStream)
    }.getOrNull()?.also { it.density=android.util.DisplayMetrics.DENSITY_DEFAULT } ?: mapPlayer(world)
    else mapPlayer(world)

internal fun mapPoi(context:Context,kind:String,world:World,night:Boolean=false):Bitmap {
    if(world==World.LIBERTY) LibertyBlips.icon(context,kind)?.let { return it }
    PoiAtlas.icon(context,kind,world,night)?.let { return it }
    if(world.mapArt().marker==MarkerArt.PIXEL && kind in setOf("food","stay"))
        return BitmapFactory.decodeResource(context.resources,if(kind=="food")R.drawable.pixel_food else R.drawable.pixel_stay)
    val b=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG);val art=world.mapArt().marker
    val accent=world.markerInk();p.color=color("#10191D")
    when(art){
        MarkerArt.PHOSPHOR->{p.color=color("#07160A");c.drawRoundRect(5f,5f,59f,59f,5f,5f,p);p.color=accent;p.style=Paint.Style.STROKE;p.strokeWidth=2.5f;c.drawRoundRect(5f,5f,59f,59f,5f,5f,p)}
        MarkerArt.SILHOUETTE,MarkerArt.SILVER->{p.color=color("#141C1D");c.drawCircle(32f,32f,27f,p);p.color=accent;p.style=Paint.Style.STROKE;p.strokeWidth=2.5f;c.drawCircle(32f,32f,27f,p)}
        MarkerArt.INK->{c.drawCircle(32f,32f,27f,p);p.color=color("#E9DCBA");p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawCircle(32f,32f,27f,p)}
        MarkerArt.PASTEL,MarkerArt.RACING->{p.color=when(kind){"food"->color(if(world==World.HORIZON)"#BC197B" else "#387C50");"cafe"->color("#D98C29");"pub"->color("#AA6129");"stay"->color("#3B83A9");"shop"->color("#A35276");"sights"->color("#6657A8");"nature"->color("#427F50");"health"->color("#B15354");"cash"->color("#438E88");else->color("#337C98")};c.drawRoundRect(5f,5f,59f,59f,if(art==MarkerArt.RACING)2f else 10f,if(art==MarkerArt.RACING)2f else 10f,p);p.color=Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawRoundRect(5f,5f,59f,59f,2f,2f,p)}
        MarkerArt.PIXEL->{p.isAntiAlias=false;p.color=color("#263226");c.drawRect(5f,5f,59f,59f,p);p.color=color("#B5C7A8");p.style=Paint.Style.STROKE;p.strokeWidth=4f;c.drawRect(5f,5f,59f,59f,p)}
        MarkerArt.GOLD->{p.color=color("#4C452D");c.drawCircle(32f,32f,25f,p);p.color=accent;p.style=Paint.Style.STROKE;p.strokeWidth=3f;c.drawCircle(32f,32f,25f,p);c.drawCircle(32f,32f,29f,p)}
        MarkerArt.TECH,MarkerArt.SHEIKAH,MarkerArt.DIAMOND,MarkerArt.SURVEY->{val path=Path().apply{moveTo(32f,2f);lineTo(62f,32f);lineTo(32f,62f);lineTo(2f,32f);close()};c.drawPath(path,p);p.color=if(art==MarkerArt.DIAMOND)color(world.routeColor) else accent;p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawPath(path,p)}
        else->Unit
    }
    val glyph=Path()
    when(kind){
        "food"->{glyph.moveTo(22f,17f);glyph.lineTo(22f,47f);glyph.moveTo(16f,17f);glyph.lineTo(16f,29f);glyph.lineTo(28f,29f);glyph.lineTo(28f,17f);glyph.moveTo(41f,17f);glyph.lineTo(41f,47f);glyph.moveTo(35f,17f);glyph.lineTo(35f,32f);glyph.lineTo(41f,32f)}
        "cafe"->{glyph.addRect(16f,23f,38f,43f,Path.Direction.CW);glyph.addRect(38f,26f,47f,36f,Path.Direction.CW);glyph.moveTo(16f,47f);glyph.lineTo(42f,47f);glyph.moveTo(23f,16f);glyph.lineTo(23f,20f);glyph.moveTo(31f,16f);glyph.lineTo(31f,20f)}
        "stay"->{glyph.moveTo(16f,19f);glyph.lineTo(16f,46f);glyph.moveTo(48f,26f);glyph.lineTo(48f,46f);glyph.addRect(16f,29f,48f,40f,Path.Direction.CW);glyph.addRect(19f,22f,29f,29f,Path.Direction.CW)}
        "fuel"->{glyph.addRect(18f,16f,36f,45f,Path.Direction.CW);glyph.addRect(21f,20f,33f,29f,Path.Direction.CW);glyph.moveTo(36f,30f);glyph.lineTo(44f,30f);glyph.lineTo(44f,42f);glyph.lineTo(49f,42f);glyph.lineTo(49f,24f);glyph.lineTo(43f,18f)}
        "parking"->{glyph.moveTo(24f,47f);glyph.lineTo(24f,17f);glyph.lineTo(42f,17f);glyph.lineTo(42f,33f);glyph.lineTo(24f,33f)}
        "pub"->{glyph.moveTo(18f,20f);glyph.lineTo(43f,20f);glyph.lineTo(39f,47f);glyph.lineTo(23f,47f);glyph.close();glyph.moveTo(19f,25f);glyph.lineTo(42f,25f);glyph.moveTo(44f,23f);glyph.lineTo(50f,23f);glyph.lineTo(48f,38f);glyph.lineTo(41f,38f)}
        "shop"->{glyph.addRect(18f,26f,46f,48f,Path.Direction.CW);glyph.moveTo(24f,27f);glyph.lineTo(24f,21f);glyph.lineTo(28f,16f);glyph.lineTo(36f,16f);glyph.lineTo(40f,21f);glyph.lineTo(40f,27f)}
        "education"->{glyph.moveTo(13f,25f);glyph.lineTo(32f,16f);glyph.lineTo(51f,25f);glyph.lineTo(32f,34f);glyph.close();glyph.moveTo(21f,30f);glyph.lineTo(21f,41f);glyph.quadTo(32f,49f,43f,41f);glyph.lineTo(43f,30f);glyph.moveTo(51f,25f);glyph.lineTo(51f,43f)}
        "sights"->{for(i in 0 until 10){val a=i*Math.PI/5;val radius=if(i%2==0)19f else 9f;val x=32f+radius*kotlin.math.sin(a).toFloat();val y=32f-radius*kotlin.math.cos(a).toFloat();if(i==0)glyph.moveTo(x,y) else glyph.lineTo(x,y)};glyph.close()}
        "health"->{glyph.moveTo(26f,15f);glyph.lineTo(38f,15f);glyph.lineTo(38f,26f);glyph.lineTo(49f,26f);glyph.lineTo(49f,38f);glyph.lineTo(38f,38f);glyph.lineTo(38f,49f);glyph.lineTo(26f,49f);glyph.lineTo(26f,38f);glyph.lineTo(15f,38f);glyph.lineTo(15f,26f);glyph.lineTo(26f,26f);glyph.close()}
        "nature"->{glyph.moveTo(32f,15f);glyph.lineTo(47f,38f);glyph.lineTo(17f,38f);glyph.close();glyph.moveTo(32f,38f);glyph.lineTo(32f,49f)}
        "cash"->{glyph.addCircle(32f,32f,18f,Path.Direction.CW);glyph.moveTo(39f,23f);glyph.lineTo(27f,23f);glyph.lineTo(24f,29f);glyph.lineTo(39f,35f);glyph.lineTo(36f,41f);glyph.lineTo(24f,41f);glyph.moveTo(32f,18f);glyph.lineTo(32f,46f)}
        else->{glyph.moveTo(15f,29f);glyph.lineTo(32f,14f);glyph.lineTo(49f,29f);glyph.moveTo(20f,28f);glyph.lineTo(20f,47f);glyph.lineTo(44f,47f);glyph.lineTo(44f,28f);glyph.addRect(28f,36f,36f,47f,Path.Direction.CW)}
    }
    p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.SQUARE;p.strokeJoin=Paint.Join.MITER
    if(art in listOf(MarkerArt.SILHOUETTE,MarkerArt.SILVER)){p.color=Color.BLACK;p.strokeWidth=7f;c.drawPath(glyph,p)}
    p.color=accent;p.strokeWidth=if(art==MarkerArt.PIXEL)4.5f else 3.3f;c.drawPath(glyph,p)
    return b
}
internal fun mapDestination(context:Context,world:World,night:Boolean):Bitmap = runCatching {
    val suffix=if(night)"_night" else ""
    val source=context.assets.open("map-markers/${world.file}$suffix.png").use(BitmapFactory::decodeStream)
        ?: return@runCatching null
    Bitmap.createScaledBitmap(source,80,80,true).also { source.recycle() }
}.getOrNull() ?: mapDestinationFallback(world)

private fun mapDestinationFallback(world:World):Bitmap {
    val b=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=color(world.routeColor);style=Paint.Style.STROKE;strokeWidth=5f}
    if(world.mapArt().marker in listOf(MarkerArt.TECH,MarkerArt.SHEIKAH,MarkerArt.DIAMOND,MarkerArt.SURVEY)) {c.drawPath(Path().apply{moveTo(40f,8f);lineTo(72f,40f);lineTo(40f,72f);lineTo(8f,40f);close()},p);c.drawLine(40f,23f,40f,57f,p);c.drawLine(23f,40f,57f,40f,p)}
    else if(world.mapArt().marker==MarkerArt.INK){c.drawCircle(40f,40f,24f,p);c.drawLine(15f,15f,65f,65f,p);c.drawLine(15f,65f,65f,15f,p)}
    else {c.drawCircle(40f,40f,24f,p);p.style=Paint.Style.FILL;c.drawCircle(40f,40f,9f,p);p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawCircle(40f,40f,31f,p)}
    return b
}

internal fun mapUnknownPosition(world:World):Bitmap {
    val b=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);val c=Canvas(b)
    val pixel=world==World.PIXEL || world==World.SAN_ANDREAS
    val p=Paint(if(pixel)0 else Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeCap=if(pixel)Paint.Cap.SQUARE else Paint.Cap.ROUND;strokeJoin=Paint.Join.MITER}
    val corners=Path().apply {
        moveTo(25f,11f);lineTo(11f,11f);lineTo(11f,25f)
        moveTo(39f,11f);lineTo(53f,11f);lineTo(53f,25f)
        moveTo(11f,39f);lineTo(11f,53f);lineTo(25f,53f)
        moveTo(53f,39f);lineTo(53f,53f);lineTo(39f,53f)
    }
    p.color=Color.BLACK;p.strokeWidth=9f;c.drawPath(corners,p)
    p.color=world.markerInk();p.strokeWidth=5f;c.drawPath(corners,p)
    return b
}
