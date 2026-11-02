package com.waypoint.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min
import kotlin.math.roundToInt

/** Waypoint's static HUD above the live car map and below host controls. */
internal class WaypointCarOverlay(private val context: Context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { isSubpixelText=true }
    private val cuePaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { isSubpixelText=true }
    private val fontCache=mutableMapOf<World,Typeface>()

    fun drawHud(canvas: Canvas,state: NavState,visible: Rect,showMapCredit: Boolean) {
        val scale=min(canvas.width/800f,canvas.height/400f).coerceIn(0.75f,2f)
        if(state.navigating && canvas.width>=600 && canvas.height>=340) drawGuidance(canvas,state,scale)
        drawWorldReadout(canvas,state,state.world,visible,scale)
        if(showMapCredit) drawMapCredit(canvas,scale)
    }

    private fun drawGuidance(canvas: Canvas,state: NavState,scale: Float) {
        val palette=hudPalette(state.world)
        val left=12f*scale;val top=12f*scale
        val right=left+350f*scale;val bottom=top+106f*scale
        paint.style=Paint.Style.FILL
        paint.color=palette.panel.toArgb()
        canvas.drawRoundRect(left,top,right,bottom,8f*scale,8f*scale,paint)
        paint.color=palette.accent.toArgb()
        canvas.drawRect(left,top,right,top+4f*scale,paint)
        paint.typeface=Typeface.DEFAULT_BOLD
        paint.textSize=16f*scale
        canvas.drawText(state.progress?.let { formatDistance(it.toTurn,state.imperial) } ?: "Ahead",
            left+70f*scale,top+30f*scale,paint)
        val maneuver=state.route?.steps?.getOrNull(state.progress?.nextStep ?: 0)
        drawTurnArrow(canvas,maneuver,left+31f*scale,top+59f*scale,scale,palette.accent.toArgb())
        val cue=when {
            !state.gpsReady -> state.gpsStatus
            state.offRoute -> "Finding a new route"
            else -> routeGuidance(state.route,state.progress).instruction
        }
        cuePaint.color=palette.foreground.toArgb()
        cuePaint.typeface=Typeface.DEFAULT_BOLD
        cuePaint.textSize=21f*scale
        val layout=StaticLayout.Builder.obtain(cue,0,cue.length,cuePaint,(270f*scale).toInt())
            .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
        canvas.save()
        canvas.translate(left+70f*scale,top+43f*scale)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTurnArrow(canvas: Canvas,maneuver: Maneuver?,x: Float,y: Float,scale: Float,color: Int) {
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=5f*scale
        paint.strokeJoin=Paint.Join.ROUND
        paint.strokeCap=Paint.Cap.ROUND
        paint.color=color
        val path=Path()
        when {
            maneuver?.type in setOf("roundabout","rotary","roundabout turn") -> {
                canvas.drawCircle(x,y+4f*scale,11f*scale,paint)
                path.moveTo(x,y+23f*scale);path.lineTo(x,y+15f*scale)
                path.moveTo(x+11f*scale,y+4f*scale);path.lineTo(x+23f*scale,y+4f*scale)
                path.moveTo(x+16f*scale,y-3f*scale);path.lineTo(x+23f*scale,y+4f*scale)
                path.lineTo(x+16f*scale,y+11f*scale)
            }
            maneuver?.modifier=="uturn" -> {
                path.moveTo(x+12f*scale,y+23f*scale);path.lineTo(x+12f*scale,y-5f*scale)
                path.lineTo(x-9f*scale,y-5f*scale);path.lineTo(x-9f*scale,y+11f*scale)
                path.moveTo(x-17f*scale,y+3f*scale);path.lineTo(x-9f*scale,y+11f*scale)
                path.lineTo(x-1f*scale,y+3f*scale)
            }
            "left" in maneuver?.modifier.orEmpty() -> {
                path.moveTo(x+14f*scale,y+23f*scale);path.lineTo(x+14f*scale,y)
                path.lineTo(x-12f*scale,y);path.moveTo(x-2f*scale,y-10f*scale)
                path.lineTo(x-12f*scale,y);path.lineTo(x-2f*scale,y+10f*scale)
            }
            "right" in maneuver?.modifier.orEmpty() -> {
                path.moveTo(x-14f*scale,y+23f*scale);path.lineTo(x-14f*scale,y)
                path.lineTo(x+12f*scale,y);path.moveTo(x+2f*scale,y-10f*scale)
                path.lineTo(x+12f*scale,y);path.lineTo(x+2f*scale,y+10f*scale)
            }
            else -> {
                path.moveTo(x,y+23f*scale);path.lineTo(x,y-13f*scale)
                path.moveTo(x-10f*scale,y-3f*scale);path.lineTo(x,y-13f*scale)
                path.lineTo(x+10f*scale,y-3f*scale)
            }
        }
        canvas.drawPath(path,paint)
        paint.style=Paint.Style.FILL
        val exit=maneuver?.exit ?: 0
        if(maneuver?.type in setOf("roundabout","rotary","roundabout turn") && exit>0) {
            paint.typeface=Typeface.DEFAULT_BOLD
            paint.textSize=13f*scale
            paint.textAlign=Paint.Align.CENTER
            canvas.drawText(exit.toString(),x,y+8f*scale,paint)
            paint.textAlign=Paint.Align.LEFT
        }
    }

    private fun drawWorldReadout(canvas: Canvas,state: NavState,world: World,visible: Rect,scale: Float) {
        if(canvas.width<600 || canvas.height<340) return
        val right=visible.right.coerceIn(0,canvas.width).toFloat().takeIf { it>canvas.width*0.7f } ?: canvas.width.toFloat()
        val bottom=visible.bottom.coerceIn(0,canvas.height).toFloat().takeIf { it>canvas.height*0.65f } ?: canvas.height.toFloat()
        val panelWidth=(if(state.navigating)200f else 188f)*scale
        val panelHeight=(if(state.navigating)86f else 65f)*scale
        val left=right-panelWidth-10f*scale
        val top=bottom-panelHeight-31f*scale
        if(left<0f || top<150f*scale) return
        val palette=hudPalette(world)
        val radius=if(world in setOf(World.NIGHT_CITY,World.HEAT,World.SPIDER))9f*scale else 3f*scale
        paint.style=Paint.Style.FILL
        paint.color=palette.panel.toArgb()
        canvas.drawRoundRect(left,top,left+panelWidth,top+panelHeight,radius,radius,paint)
        paint.color=palette.accent.toArgb()
        canvas.drawRect(left+10f*scale,top+9f*scale,left+43f*scale,top+12f*scale,paint)
        paint.typeface=Typeface.DEFAULT_BOLD
        paint.textSize=11f*scale
        paint.color=palette.muted.toArgb()
        canvas.drawText("WAYPOINT",left+10f*scale,top+27f*scale,paint)
        paint.typeface=fontCache.getOrPut(world) { context.resources.getFont(worldCarFont(world)) }
        paint.textSize=23f*scale
        while(paint.measureText(world.title)>panelWidth-20f*scale && paint.textSize>15f*scale)
            paint.textSize-=1f*scale
        paint.color=palette.foreground.toArgb()
        canvas.drawText(world.title,left+10f*scale,top+51f*scale,paint)
        if(state.navigating) {
            val speed=if(state.gpsReady) (state.speed*(if(state.imperial)2.23694f else 3.6f)).roundToInt().toString() else "—"
            paint.typeface=Typeface.DEFAULT_BOLD
            paint.textSize=14f*scale
            paint.color=palette.accent.toArgb()
            canvas.drawText("$speed ${if(state.imperial)"mph" else "km/h"}",left+10f*scale,top+74f*scale,paint)
        }
    }

    private fun drawMapCredit(canvas: Canvas,scale: Float) {
        val label="Map data: OpenStreetMap"
        paint.style=Paint.Style.FILL
        paint.typeface=Typeface.DEFAULT
        paint.textSize=11f*scale
        val textWidth=paint.measureText(label)
        val x=10f*scale
        val top=canvas.height-22f*scale
        paint.color=Color.argb(220,15,18,19)
        canvas.drawRoundRect(x-5f*scale,top-1f*scale,x+textWidth+5f*scale,canvas.height.toFloat()-2f*scale,3f*scale,3f*scale,paint)
        paint.color=Color.WHITE
        canvas.drawText(label,x,canvas.height-7f*scale,paint)
    }

    private fun worldCarFont(world: World)=when(world) {
        World.LOS_SANTOS,World.SAN_ANDREAS,World.LIBERTY -> R.font.pricedown_black
        World.OLD_WEST -> R.font.chinese_rocks_regular
        World.VICE_COAST -> R.font.yellowtail
        World.PIXEL -> R.font.monocraft
        World.GREEN_TERMINAL -> R.font.share_tech_mono
        World.NIGHT_CITY,World.BRIDGES,World.SPIDER,World.STARFIELD -> R.font.rajdhani
        World.LANDS_BETWEEN,World.HYRULE,World.ASSASSIN,World.HORIZON_WILDS,World.HOGWARTS,World.KINGDOM,World.MAFIA -> R.font.cinzel
        else -> R.font.barlow_condensed_bold
    }
}
