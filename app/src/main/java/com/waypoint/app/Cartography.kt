package com.waypoint.app

import android.content.Context
import android.graphics.*

/** Independent, geographic rendering rules; all points and building heights stay real. */
data class MapArt(val tilt:Double,val zoom:Double,val routeWidth:Float,val poiScale:Float,val marker:MarkerArt,val player:PlayerArt,val lightLabels:Boolean)
enum class MarkerArt { SILHOUETTE, INK, TECH, PIXEL, PHOSPHOR, PASTEL, SHEIKAH, SILVER, GOLD, RACING, DIAMOND, SURVEY }
enum class PlayerArt { ARROW, TEARDROP, CHEVRON, PIXEL, POINTER, RING }
fun World.mapArt()=when(this) {
    World.LOS_SANTOS->MapArt(0.0,16.2,5f,.55f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.OLD_WEST->MapArt(0.0,16.0,3.8f,.55f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.NIGHT_CITY->MapArt(45.0,16.2,3.8f,.55f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.PIXEL->MapArt(42.0,17.0,5f,.6f,MarkerArt.PIXEL,PlayerArt.PIXEL,true)
    World.GREEN_TERMINAL->MapArt(0.0,16.1,3f,.6f,MarkerArt.PHOSPHOR,PlayerArt.POINTER,true)
    World.SAN_ANDREAS->MapArt(0.0,16.0,5f,.7f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,false)
    World.HEAT->MapArt(0.0,15.8,4.5f,.65f,MarkerArt.RACING,PlayerArt.ARROW,true)
    World.RIVIA->MapArt(0.0,15.6,3f,.58f,MarkerArt.GOLD,PlayerArt.POINTER,false)
    World.VICE_COAST->MapArt(0.0,16.0,5f,.7f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.LIBERTY->MapArt(0.0,16.1,4.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.CTOS->MapArt(0.0,16.0,4f,.62f,MarkerArt.PASTEL,PlayerArt.RING,false)
    World.HYRULE->MapArt(0.0,15.7,2.5f,.55f,MarkerArt.SHEIKAH,PlayerArt.POINTER,true)
    World.SKYRIM->MapArt(35.0,15.5,2f,.62f,MarkerArt.SILVER,PlayerArt.POINTER,true)
    World.LANDS_BETWEEN->MapArt(0.0,15.6,2.5f,.65f,MarkerArt.GOLD,PlayerArt.RING,false)
    World.HORIZON->MapArt(0.0,15.9,4.5f,.65f,MarkerArt.RACING,PlayerArt.ARROW,true)
    World.ASSASSIN->MapArt(0.0,15.6,2.5f,.55f,MarkerArt.GOLD,PlayerArt.POINTER,true)
    World.FAR_CRY->MapArt(30.0,15.7,3.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.GHOST->MapArt(0.0,15.6,2.5f,.55f,MarkerArt.DIAMOND,PlayerArt.POINTER,false)
    World.BRIDGES->MapArt(25.0,15.7,2.5f,.5f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.HORIZON_WILDS->MapArt(0.0,15.6,3f,.55f,MarkerArt.SILVER,PlayerArt.POINTER,true)
    World.SPIDER->MapArt(40.0,16.1,4f,.55f,MarkerArt.DIAMOND,PlayerArt.CHEVRON,true)
    World.HOGWARTS->MapArt(25.0,15.5,2.5f,.6f,MarkerArt.GOLD,PlayerArt.RING,true)
    World.STARFIELD->MapArt(30.0,15.9,2.5f,.5f,MarkerArt.SURVEY,PlayerArt.POINTER,true)
    World.KINGDOM->MapArt(0.0,15.6,3f,.65f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.SAINTS->MapArt(0.0,16.0,4f,.6f,MarkerArt.PASTEL,PlayerArt.ARROW,true)
    World.JUST_CAUSE->MapArt(0.0,15.6,3f,.55f,MarkerArt.DIAMOND,PlayerArt.POINTER,true)
    World.MAFIA->MapArt(0.0,16.0,4f,.6f,MarkerArt.INK,PlayerArt.ARROW,false)
    World.DIVISION->MapArt(40.0,16.0,4f,.6f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.SEA_CHART->MapArt(0.0,15.6,3.5f,.65f,MarkerArt.INK,PlayerArt.TEARDROP,false)
    World.DOS_ATLAS->MapArt(0.0,16.0,3f,.55f,MarkerArt.PHOSPHOR,PlayerArt.POINTER,true)
    World.AINCRAD->MapArt(0.0,15.8,3.5f,.6f,MarkerArt.GOLD,PlayerArt.RING,false)
    World.COUNTER_STRIKE->MapArt(0.0,16.7,4f,.65f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.DEFCON->MapArt(0.0,15.5,2.5f,.5f,MarkerArt.TECH,PlayerArt.CHEVRON,true)
    World.FIELD_OPS->MapArt(0.0,16.2,3.5f,.6f,MarkerArt.SILHOUETTE,PlayerArt.ARROW,true)
    World.RETRO_QUEST->MapArt(0.0,16.0,4f,.6f,MarkerArt.PIXEL,PlayerArt.PIXEL,false)
    World.NIER->MapArt(42.0,16.0,4.5f,.6f,MarkerArt.DIAMOND,PlayerArt.RING,false)
    World.STALKER->MapArt(0.0,15.8,4f,.6f,MarkerArt.PHOSPHOR,PlayerArt.POINTER,true)
    World.HOME_CONSOLE->MapArt(25.0,16.0,4f,.6f,MarkerArt.PASTEL,PlayerArt.RING,false)
}
internal fun mapCameraTilt(world:World,enabled:Boolean,navigating:Boolean=false,previewTilt:Double?=null):Double =
    previewTilt ?: if(!enabled) 0.0 else if(navigating) 50.0 else world.mapArt().tilt.takeIf { it>0.0 } ?: 45.0
private fun color(hex:String)=Color.parseColor(hex)
private fun World.markerInk()=when(mapArt().marker){MarkerArt.PHOSPHOR->color("#A4F780");MarkerArt.TECH,MarkerArt.SHEIKAH->color("#7DDDEC");MarkerArt.GOLD->color("#ECD797");else->Color.WHITE}
private object PoiAtlas {
    private val kinds=listOf("place","food","cafe","pub","stay","fuel","parking","shop","sights","health","nature","cash")
    private var loadedWorld:String?=null
    private var loaded:Bitmap?=null

    @Synchronized fun icon(context:Context,kind:String,world:World):Bitmap? {
        val index=kinds.indexOf(kind)
        if(index<0)return null
        if(loadedWorld!=world.file) {
            loadedWorld=world.file
            loaded=runCatching {context.assets.open("poi/${world.file}.webp").use(BitmapFactory::decodeStream)}.getOrNull()
        }
        val atlas=loaded ?: return null
        if(atlas.width!=512 || atlas.height!=384)return null
        val tile=Bitmap.createBitmap(atlas,(index%4)*128,(index/4)*128,128,128)
        return Bitmap.createScaledBitmap(tile,64,64,world!=World.PIXEL && world!=World.SAN_ANDREAS)
    }
}
internal fun mapPlayer(world:World):Bitmap {
    val b=Bitmap.createBitmap(96,104,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG);val ink=if(world==World.SAINTS)color("#FFDA45") else world.markerInk()
    when(world.mapArt().player) {
        PlayerArt.TEARDROP->{val path=Path().apply{moveTo(48f,8f);cubicTo(28f,34f,12f,47f,19f,70f);cubicTo(28f,99f,71f,99f,77f,70f);cubicTo(83f,47f,68f,34f,48f,8f);close()};p.color=Color.BLACK;p.style=Paint.Style.STROKE;p.strokeWidth=10f;c.drawPath(path,p);p.style=Paint.Style.FILL;p.color=Color.WHITE;c.drawPath(path,p);p.color=Color.BLACK;c.drawCircle(48f,65f,16f,p)}
        PlayerArt.PIXEL->{p.isAntiAlias=false;p.color=Color.BLACK;c.drawRect(38f,8f,58f,28f,p);c.drawRect(30f,28f,66f,86f,p);c.drawRect(22f,44f,74f,74f,p);p.color=Color.WHITE;c.drawRect(42f,16f,54f,78f,p);c.drawRect(30f,52f,66f,66f,p)}
        PlayerArt.RING->{p.color=Color.BLACK;c.drawCircle(48f,54f,27f,p);p.color=ink;p.style=Paint.Style.STROKE;p.strokeWidth=5f;c.drawCircle(48f,54f,22f,p);p.style=Paint.Style.FILL;c.drawPath(Path().apply{moveTo(48f,9f);lineTo(39f,29f);lineTo(57f,29f);close()},p);c.drawCircle(48f,54f,6f,p)}
        else->{val path=Path().apply{moveTo(48f,9f);lineTo(81f,87f);lineTo(48f,68f);lineTo(15f,87f);close()};p.color=Color.BLACK;p.style=Paint.Style.STROKE;p.strokeWidth=9f;p.strokeJoin=Paint.Join.ROUND;c.drawPath(path,p);p.color=ink;p.style=if(world.mapArt().player==PlayerArt.CHEVRON)Paint.Style.STROKE else Paint.Style.FILL;p.strokeWidth=7f;c.drawPath(path,p)}
    }
    return b
}
internal fun mapPoi(context:Context,kind:String,world:World):Bitmap {
    if(kind=="education") {
        if(world==World.SAN_ANDREAS) {
            runCatching {context.assets.open("poi/san_andreas_education.webp").use(BitmapFactory::decodeStream)}.getOrNull()?.let {return it}
        }
    }
    PoiAtlas.icon(context,kind,world)?.let { return it }
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
internal fun mapDestination(world:World):Bitmap {
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
