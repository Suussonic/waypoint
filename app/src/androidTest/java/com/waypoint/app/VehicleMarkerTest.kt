package com.waypoint.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.DisplayMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleMarkerTest {
    @Test fun everyWorldHasUnclippedCarAndMotorcycleArtworkAtCarDisplayDensity() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(context.getExternalFilesDir(null),"vehicle-markers").apply { mkdirs() }
        val gallery=Bitmap.createBitmap(1800,((World.entries.size+4)/5)*216,Bitmap.Config.ARGB_8888).apply { density=DisplayMetrics.DENSITY_DEFAULT }
        val canvas=Canvas(gallery)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        World.entries.forEachIndexed { index,world ->
            val x=index%5*360f;val y=index/5*216f
            val dark=index%2==0
            paint.color=if(dark)Color.rgb(28,39,43) else Color.rgb(231,224,201)
            canvas.drawRect(x,y,x+360,y+216,paint)
            paint.color=if(dark)Color.WHITE else Color.rgb(26,35,39)
            paint.textSize=14f
            canvas.drawText(world.title,x+12,y+24,paint)
            listOf(TravelMode.CAR,TravelMode.MOTORCYCLE,TravelMode.BICYCLE).forEachIndexed { column,mode ->
                val bitmap=mapVehicle(world,mode)
                assertEquals("${world.file} $mode density",DisplayMetrics.DENSITY_DEFAULT,bitmap.density)
                assertEquals(96,bitmap.width);assertEquals(104,bitmap.height)
                val pixels=IntArray(96*104);bitmap.getPixels(pixels,0,96,0,0,96,104)
                assertTrue("${world.file} $mode is visible",pixels.count { Color.alpha(it)>0 }>400)
                for(px in 0 until 96) {
                    assertEquals("${world.file} $mode clipped top",0,Color.alpha(bitmap.getPixel(px,0)))
                    assertEquals("${world.file} $mode clipped bottom",0,Color.alpha(bitmap.getPixel(px,103)))
                }
                for(py in 0 until 104) {
                    assertEquals("${world.file} $mode clipped side",0,Color.alpha(bitmap.getPixel(0,py)))
                    assertEquals("${world.file} $mode clipped side",0,Color.alpha(bitmap.getPixel(95,py)))
                }
                canvas.drawBitmap(bitmap,x+12+column*120,y+44,null)
                paint.textSize=12f
                canvas.drawText(mode.label.uppercase(),x+12+column*120,y+181,paint)
                File(folder,"${world.file}-${mode.name.lowercase()}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                }
                bitmap.recycle()
            }
        }
        File(folder,"all-world-vehicles.png").outputStream().use { gallery.compress(Bitmap.CompressFormat.PNG,100,it) }
        gallery.recycle()
    }

    @Test fun themedMountsAndTravelModeSelectionReplaceOnlyKnownHeadingArrows() {
        assertEquals(WorldTransport.HORSE,World.OLD_WEST.vehicleLivery().car)
        assertEquals(WorldTransport.MINECART,World.PIXEL.vehicleLivery().car)
        assertEquals(WorldTransport.SHIP,World.SEA_CHART.vehicleLivery().car)
        assertEquals(WorldTransport.BROOM,World.HOGWARTS.vehicleLivery().bike)
        assertEquals("vehicle-car",playerMarkerImage(TravelMode.CAR,true))
        assertEquals("vehicle-bike",playerMarkerImage(TravelMode.MOTORCYCLE,true))
        assertEquals("vehicle-bicycle",playerMarkerImage(TravelMode.BICYCLE,true))
        assertEquals("nav-arrow",playerMarkerImage(TravelMode.WALK,true))
        assertEquals("position-uncertain",playerMarkerImage(TravelMode.CAR,false))
    }
}
