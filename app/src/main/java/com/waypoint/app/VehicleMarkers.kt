package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.DisplayMetrics

/** Original transport illustrations. World names describe inspiration, not licensed game assets. */
internal enum class WorldTransport { CAR, SPORTS_CAR, CLASSIC_CAR, TRUCK, ROVER, MOTORCYCLE, TRIKE,
    HORSE, HORNED_STEED, MACHINE_MOUNT, CAMEL, MINECART, PIG, SHIP, ROWBOAT, CARRIAGE, BROOM, FLIGHT_UNIT, POD, BICYCLE }

internal data class VehicleLivery(val car: WorldTransport,val bike: WorldTransport,
    val body: String,val trim: String,val pixel: Boolean=false,val wire: Boolean=false)

internal fun World.vehicleLivery(): VehicleLivery = when(this) {
    World.LOS_SANTOS -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#CAD8B5","#8060B7")
    World.OLD_WEST -> VehicleLivery(WorldTransport.HORSE,WorldTransport.HORSE,"#AE754C","#EED5A3")
    World.NIGHT_CITY -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#E6CF44","#68E7ED")
    World.PIXEL -> VehicleLivery(WorldTransport.MINECART,WorldTransport.PIG,"#BCC9C3","#6B8C57",pixel=true)
    World.GREEN_TERMINAL -> VehicleLivery(WorldTransport.ROVER,WorldTransport.MOTORCYCLE,"#203622","#B2F58F",wire=true)
    World.SAN_ANDREAS -> VehicleLivery(WorldTransport.CLASSIC_CAR,WorldTransport.MOTORCYCLE,"#7BC46B","#F2E0BC")
    World.HEAT -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#E555B8","#A3F4ED")
    World.RIVIA -> VehicleLivery(WorldTransport.HORSE,WorldTransport.HORSE,"#9D6142","#D8B975")
    World.VICE_COAST -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#F1A5CD","#74D5D5")
    World.LIBERTY -> VehicleLivery(WorldTransport.CAR,WorldTransport.MOTORCYCLE,"#DCA84A","#E6E4D7")
    World.CTOS -> VehicleLivery(WorldTransport.CAR,WorldTransport.MOTORCYCLE,"#E6EAE9","#51A3DB")
    World.HYRULE -> VehicleLivery(WorldTransport.HORSE,WorldTransport.TRIKE,"#C29C6A","#85DCEB")
    World.SKYRIM -> VehicleLivery(WorldTransport.HORSE,WorldTransport.HORSE,"#A6B3B5","#E8E6DA")
    World.LANDS_BETWEEN -> VehicleLivery(WorldTransport.HORNED_STEED,WorldTransport.HORNED_STEED,"#A3A89B","#E8CF7C")
    World.HORIZON -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#EF719D","#F9E48D")
    World.ASSASSIN -> VehicleLivery(WorldTransport.CAMEL,WorldTransport.HORSE,"#D3AE76","#F5E6C3")
    World.FAR_CRY -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#D5B55B","#D7DCC6")
    World.GHOST -> VehicleLivery(WorldTransport.HORSE,WorldTransport.HORSE,"#D9D8C8","#B95545")
    World.BRIDGES -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.TRIKE,"#749AAB","#F4D875")
    World.HORIZON_WILDS -> VehicleLivery(WorldTransport.MACHINE_MOUNT,WorldTransport.MACHINE_MOUNT,"#C9D5D3","#69B8D5")
    World.SPIDER -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#D95452","#8DCDEB")
    World.HOGWARTS -> VehicleLivery(WorldTransport.CARRIAGE,WorldTransport.BROOM,"#91734D","#EAD69C")
    World.STARFIELD -> VehicleLivery(WorldTransport.ROVER,WorldTransport.TRIKE,"#E0DDCC","#DC8D63")
    World.KINGDOM -> VehicleLivery(WorldTransport.HORSE,WorldTransport.HORSE,"#BD9063","#E9CE92")
    World.SAINTS -> VehicleLivery(WorldTransport.CAR,WorldTransport.MOTORCYCLE,"#B388D4","#EED67C")
    World.JUST_CAUSE -> VehicleLivery(WorldTransport.SPORTS_CAR,WorldTransport.MOTORCYCLE,"#C94E48","#D0E5DD")
    World.MAFIA -> VehicleLivery(WorldTransport.CLASSIC_CAR,WorldTransport.MOTORCYCLE,"#88474C","#EDD5A1")
    World.DIVISION -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#6F8583","#FFA351")
    World.SEA_CHART -> VehicleLivery(WorldTransport.SHIP,WorldTransport.ROWBOAT,"#A37146","#F0DDB1")
    World.DOS_ATLAS -> VehicleLivery(WorldTransport.CAR,WorldTransport.MOTORCYCLE,"#473822","#FFE1A0",pixel=true,wire=true)
    World.AINCRAD -> VehicleLivery(WorldTransport.CARRIAGE,WorldTransport.HORSE,"#E5E8DF","#E1AE57")
    World.COUNTER_STRIKE -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#ADAF89","#8CDAEC")
    World.DEFCON -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#172A43","#B0D5FF",wire=true)
    World.FIELD_OPS -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#A5B38A","#E8E7BC")
    World.RETRO_QUEST -> VehicleLivery(WorldTransport.CARRIAGE,WorldTransport.HORSE,"#B68EC5","#F7DE8B",pixel=true)
    World.STALKER -> VehicleLivery(WorldTransport.TRUCK,WorldTransport.MOTORCYCLE,"#7B8065","#E2D9B6")
    World.NIER -> VehicleLivery(WorldTransport.FLIGHT_UNIT,WorldTransport.POD,"#C3C1AC","#8C503D")
    World.HOME_CONSOLE -> VehicleLivery(WorldTransport.CAR,WorldTransport.MOTORCYCLE,"#67BBD7","#F4F7EB")
}

internal fun playerMarkerImage(mode: TravelMode,headingKnown: Boolean): String = when {
    !headingKnown -> "position-uncertain"
    mode==TravelMode.CAR -> "vehicle-car"
    mode==TravelMode.MOTORCYCLE -> "vehicle-bike"
    mode==TravelMode.BICYCLE -> "vehicle-bicycle"
    else -> "nav-arrow"
}

internal fun mapVehicle(world: World,mode: TravelMode): Bitmap {
    val livery=world.vehicleLivery()
    val transport=when(mode) { TravelMode.BICYCLE -> WorldTransport.BICYCLE; TravelMode.MOTORCYCLE -> livery.bike; else -> livery.car }
    val bitmap=Bitmap.createBitmap(if(livery.pixel)24 else 96,if(livery.pixel)26 else 104,Bitmap.Config.ARGB_8888)
    bitmap.density=DisplayMetrics.DENSITY_DEFAULT
    val canvas=Canvas(bitmap)
    if(livery.pixel)canvas.scale(.25f,.25f)
    // Leave a full transparent pixel even after downsampling the widest outline.
    canvas.translate(4f,4f)
    canvas.scale(88f/96f,96f/104f)
    VehiclePainter(canvas,livery).draw(transport)
    return if(livery.pixel)Bitmap.createScaledBitmap(bitmap,96,104,false).also {
        it.density=DisplayMetrics.DENSITY_DEFAULT;bitmap.recycle()
    } else bitmap
}

/** Drawn facing up; every silhouette has a visible front and a double outline for light/dark maps. */
private class VehiclePainter(val canvas: Canvas,val livery: VehicleLivery) {
    private val paint=Paint(if(livery.pixel)0 else Paint.ANTI_ALIAS_FLAG).apply {
        strokeJoin=Paint.Join.ROUND;strokeCap=Paint.Cap.ROUND
    }
    private val ink=Color.rgb(22,29,31)
    private val light=Color.rgb(246,241,216)
    private val body=Color.parseColor(livery.body)
    private val trim=Color.parseColor(livery.trim)
    private fun path(vararg xy: Float)=Path().apply {
        moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()
    }
    private fun shape(path: Path,fill: Int=body,outline: Boolean=true) {
        if(outline) {
            paint.style=Paint.Style.STROKE;paint.strokeWidth=8f;paint.color=light;canvas.drawPath(path,paint)
            paint.strokeWidth=5f;paint.color=ink;canvas.drawPath(path,paint)
        }
        paint.style=Paint.Style.FILL;paint.color=fill;canvas.drawPath(path,paint)
        if(livery.wire && fill==body) {
            paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=trim;canvas.drawPath(path,paint)
        }
    }
    private fun box(l: Float,t: Float,r: Float,b: Float,fill: Int=body,radius: Float=0f,outline: Boolean=true) =
        shape(Path().apply { addRoundRect(l,t,r,b,radius,radius,Path.Direction.CW) },fill,outline)
    private fun oval(l: Float,t: Float,r: Float,b: Float,fill: Int=body,outline: Boolean=true) =
        shape(Path().apply { addOval(l,t,r,b,Path.Direction.CW) },fill,outline)
    private fun line(x1: Float,y1: Float,x2: Float,y2: Float,color: Int=trim,width: Float=3f) {
        paint.style=Paint.Style.STROKE;paint.strokeWidth=width;paint.color=color
        canvas.drawLine(x1,y1,x2,y2,paint)
    }

    fun draw(kind: WorldTransport) {
        when(kind) {
            WorldTransport.HORSE,WorldTransport.HORNED_STEED,WorldTransport.MACHINE_MOUNT,WorldTransport.CAMEL -> mount(kind)
            WorldTransport.MINECART -> minecart()
            WorldTransport.PIG -> pig()
            WorldTransport.SHIP,WorldTransport.ROWBOAT -> boat(kind==WorldTransport.SHIP)
            WorldTransport.CARRIAGE -> carriage()
            WorldTransport.BROOM -> broom()
            WorldTransport.FLIGHT_UNIT,WorldTransport.POD -> machine(kind==WorldTransport.FLIGHT_UNIT)
            WorldTransport.BICYCLE -> bicycle()
            WorldTransport.MOTORCYCLE,WorldTransport.TRIKE -> motorcycle(kind==WorldTransport.TRIKE)
            else -> car(kind)
        }
    }
    private fun car(kind: WorldTransport) {
        val rugged=kind==WorldTransport.TRUCK || kind==WorldTransport.ROVER
        val classic=kind==WorldTransport.CLASSIC_CAR
        for(x in listOf(19f,68f)) for(y in listOf(24f,67f))box(x,y,x+9,y+17,ink,3f)
        if(kind==WorldTransport.ROVER)for(x in listOf(18f,69f))box(x,47f,x+9,59f,ink,3f)
        val hull=when {
            rugged -> path(29f,13f,67f,13f,73f,25f,73f,88f,23f,88f,23f,25f)
            classic -> Path().apply { addRoundRect(27f,10f,69f,92f,17f,17f,Path.Direction.CW) }
            kind==WorldTransport.SPORTS_CAR -> path(34f,10f,62f,10f,72f,28f,70f,87f,26f,87f,24f,28f)
            else -> Path().apply { addRoundRect(26f,12f,70f,91f,14f,14f,Path.Direction.CW) }
        }
        shape(hull)
        box(31f,31f,65f,48f,ink,4f,false)
        line(34f,34f,61f,34f,trim,4f)
        box(33f,55f,63f,74f,if(rugged)ink else trim,4f,false)
        line(33f,21f,39f,21f,light,4f);line(57f,21f,63f,21f,light,4f)
        if(rugged) {
            line(29f,58f,67f,58f,trim,4f);line(29f,70f,67f,70f,trim,4f)
            if(kind==WorldTransport.TRUCK)line(47f,52f,47f,77f,body,5f)
        }
        if(kind==WorldTransport.SPORTS_CAR) {
            box(22f,82f,74f,88f,trim,1f)
            line(45f,13f,45f,28f,trim,3f);line(51f,13f,51f,28f,trim,3f)
        }
        if(classic) { line(35f,79f,61f,79f,light,3f);line(47f,12f,47f,27f,trim,3f) }
    }
    private fun motorcycle(trike: Boolean) {
        box(42f,7f,54f,31f,ink,5f)
        if(trike)for(x in listOf(23f,61f))box(x,73f,x+12,96f,ink,4f)
        else box(41f,75f,55f,98f,ink,5f)
        line(43f,25f,40f,43f,light,5f);line(53f,25f,56f,43f,light,5f)
        shape(path(48f,29f,64f,42f,62f,77f,55f,87f,41f,87f,34f,77f,32f,42f))
        box(30f,28f,66f,34f,trim,3f)
        oval(39f,38f,57f,56f,trim,false)
        box(40f,61f,56f,78f,ink,5f,false)
        if(trike) { box(24f,54f,34f,78f,trim,2f);box(62f,54f,72f,78f,trim,2f) }
        line(44f,31f,52f,31f,light,3f)
    }
    private fun mount(kind: WorldTransport) {
        val machine=kind==WorldTransport.MACHINE_MOUNT
        // Splayed legs and muzzle remain legible at dashboard scale.
        for(x in listOf(25f,58f)) {
            box(x,42f,x+12,57f,body,4f)
            box(x,71f,x+12,86f,body,4f)
            line(x+3,83f,x+9,83f,ink,4f)
        }
        shape(path(43f,79f,53f,79f,58f,98f,45f,92f,39f,99f),ink)
        if(machine)shape(path(34f,35f,62f,35f,69f,58f,61f,83f,35f,83f,27f,58f))
        else oval(29f,33f,67f,86f)
        box(37f,43f,59f,64f,trim,5f,false)
        if(kind==WorldTransport.CAMEL)oval(39f,45f,57f,68f,body,false)
        shape(path(36f,11f,42f,18f,48f,14f,54f,18f,60f,11f,58f,30f,56f,41f,40f,41f,38f,30f))
        oval(39f,22f,57f,40f,if(machine)trim else body,false)
        line(42f,28f,54f,28f,ink,3f)
        if(kind==WorldTransport.HORNED_STEED) {
            line(39f,20f,31f,9f,light,5f);line(57f,20f,65f,9f,light,5f)
        }
        if(machine) { line(28f,56f,38f,56f,trim,4f);line(58f,56f,68f,56f,trim,4f) }
    }
    private fun minecart() {
        for(x in listOf(19f,68f))for(y in listOf(29f,70f))box(x,y,x+9,y+13,ink)
        shape(path(22f,25f,35f,12f,64f,12f,76f,25f,72f,88f,26f,88f))
        box(29f,27f,67f,71f,ink)
        box(35f,32f,61f,58f,trim,0f,false)
        box(25f,73f,73f,87f,body)
        line(31f,78f,65f,78f,light,3f)
        line(35f,19f,61f,19f,light,4f)
    }
    private fun pig() {
        val pink=Color.rgb(228,151,159)
        for(x in listOf(24f,59f))for(y in listOf(42f,74f))box(x,y,x+13,y+13,Color.rgb(163,92,113))
        box(27f,36f,69f,87f,pink,7f)
        box(28f,15f,39f,30f,pink);box(57f,15f,68f,30f,pink)
        box(31f,20f,65f,46f,pink,4f)
        box(38f,21f,58f,31f,Color.rgb(242,182,177),0f,false)
        box(39f,33f,43f,37f,ink,0f,false);box(53f,33f,57f,37f,ink,0f,false)
        box(35f,51f,61f,68f,Color.rgb(125,79,48),0f,false)
        line(44f,88f,53f,94f,pink,5f)
    }
    private fun boat(sails: Boolean) {
        shape(Path().apply { moveTo(48f,7f);cubicTo(81f,35f,78f,75f,64f,94f);lineTo(32f,94f);cubicTo(18f,75f,15f,35f,48f,7f);close() })
        shape(path(48f,23f,65f,45f,62f,81f,34f,81f,31f,45f),trim,false)
        for(y in listOf(49f,65f,81f))line(31f,y,65f,y,body,4f)
        if(sails) {
            line(48f,19f,48f,87f,ink,4f)
            shape(path(24f,35f,69f,35f,61f,61f,29f,61f),light)
            line(47f,36f,47f,59f,body,2f)
        } else {
            line(14f,77f,35f,43f,ink,7f);line(14f,77f,35f,43f,light,3f)
            line(82f,77f,61f,43f,ink,7f);line(82f,77f,61f,43f,light,3f)
        }
    }
    private fun carriage() {
        for(x in listOf(18f,67f))for(y in listOf(29f,67f))box(x,y,x+11,y+22,ink,5f)
        line(39f,11f,39f,32f,trim,6f);line(57f,11f,57f,32f,trim,6f)
        box(27f,27f,69f,91f,body,10f)
        box(31f,35f,65f,70f,trim,9f,false)
        line(32f,42f,64f,42f,body,3f);line(32f,62f,64f,62f,body,3f)
        box(35f,79f,61f,86f,ink,2f,false)
    }
    private fun machine(wings: Boolean) {
        if(wings) {
            shape(path(35f,34f,9f,49f,9f,70f,34f,61f))
            shape(path(61f,34f,87f,49f,87f,70f,62f,61f))
            box(24f,65f,38f,91f,trim,2f);box(58f,65f,72f,91f,trim,2f)
        } else {
            line(25f,46f,17f,74f,light,7f);line(71f,46f,79f,74f,light,7f)
            box(14f,71f,24f,82f,trim,2f);box(72f,71f,82f,82f,trim,2f)
        }
        shape(path(36f,13f,60f,13f,65f,30f,63f,69f,33f,69f,31f,30f))
        box(38f,22f,58f,30f,ink,0f,false)
        line(39f,25f,57f,25f,trim,2f)
        box(40f,43f,56f,57f,trim,0f,false)
        line(40f,75f,40f,88f,ink,5f);line(56f,75f,56f,88f,ink,5f)
    }
    private fun bicycle() {
        // Narrow wheels, an open frame and pedals distinguish cycling from the motorbike.
        box(44f,8f,52f,33f,ink,4f);box(44f,73f,52f,98f,ink,4f)
        line(48f,25f,48f,82f,light,9f);line(48f,25f,48f,82f,body,5f)
        line(48f,34f,32f,61f,light,8f);line(32f,61f,48f,77f,light,8f)
        line(48f,34f,32f,61f,body,4f);line(32f,61f,48f,77f,body,4f)
        box(30f,28f,66f,34f,trim,3f)
        box(37f,56f,59f,64f,body,4f)
        line(32f,67f,64f,67f,ink,4f)
        box(25f,64f,35f,70f,trim,1f);box(61f,64f,71f,70f,trim,1f)
    }
    private fun broom() {
        shape(path(45f,8f,53f,8f,52f,67f,42f,67f),trim)
        shape(path(39f,58f,58f,58f,70f,93f,57f,90f,47f,98f,37f,91f,25f,95f),body)
        line(37f,67f,60f,67f,trim,5f)
        line(44f,73f,39f,89f,trim,2f);line(51f,73f,54f,88f,trim,2f)
    }
}
