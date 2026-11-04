package com.waypoint.app

import android.graphics.Rect
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.SymbolLayer
import java.io.File
import kotlin.math.abs

/** Public synthetic fixes; repeated sensor emissions must not delay accepted GPS movement. */
class CarMotionContinuityTest {
    @Test fun gpsMotionKeepsMovingBetweenFixesAndIgnoresDuplicateTargets() {
        val inst=InstrumentationRegistry.getInstrumentation()
        check(inst.targetContext.packageName.endsWith(".preview"))
        val baseline=InstrumentationRegistry.getArguments().getString("motionBaseline")=="true"
        val result=JSONArray()
        val renderFps=java.util.Collections.synchronizedList(mutableListOf<Double>())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var view:WaypointCarLiveMap
            val mapField=WaypointCarLiveMap::class.java.getDeclaredField("map").apply { isAccessible=true }
            val worldField=WaypointCarLiveMap::class.java.getDeclaredField("loadedWorld").apply { isAccessible=true }
            val positionField=WaypointCarLiveMap::class.java.getDeclaredField("renderedPosition").apply { isAccessible=true }
            var state=NavState(world=World.STALKER,fix=Coordinate(51.5081,-.1280),navigating=true,
                headingReliable=true,headingSource=HeadingSource.COURSE,bearing=0f,speed=8f,gpsReady=true)
            fun update() { scenario.onActivity {
                val w=view.width.coerceAtLeast(1);val h=view.height.coerceAtLeast(1)
                view.update(state,carCamera(state,w,h),Rect(0,0,w,h),false,true)
            } }
            scenario.onActivity { activity ->
                view=WaypointCarLiveMap(activity,WaypointCarOverlay(activity))
                activity.setContentView(view);view.start()
            }
            try {
                val deadline=SystemClock.elapsedRealtime()+15_000
                var ready=false
                while(!ready && SystemClock.elapsedRealtime()<deadline) {
                    update()
                    scenario.onActivity { ready=worldField.get(view)==World.STALKER }
                    SystemClock.sleep(100)
                }
                assertTrue("Car map became ready",ready)
                SystemClock.sleep(1600)
                scenario.onActivity {
                    (mapField.get(view) as MapLibreMap).setOnFpsChangedListener { fps -> renderFps.add(fps) }
                }
                for(noisy in listOf(false,true)) {
                    repeat(3) { step ->
                        val from=state.fix!!
                        val target=Coordinate(from.lat+.0001,from.lon)
                        state=state.copy(fix=target,bearing=if(step%2==0)40f else 0f)
                        val start=SystemClock.elapsedRealtime()
                        update()
                        var at700=0.0;var at950=0.0;var maxHeadingGap=0f
                        for(sample in 1..20) {
                            SystemClock.sleep((start+sample*50-SystemClock.elapsedRealtime()).coerceAtLeast(0))
                            if(noisy) update()
                            scenario.onActivity {
                                val point=positionField.get(view) as Coordinate
                                val fraction=(point.lat-from.lat)/(target.lat-from.lat)
                                if(sample==14)at700=fraction
                                if(sample==19)at950=fraction
                                val map=mapField.get(view) as MapLibreMap
                                val marker=map.style!!.getLayerAs<SymbolLayer>("car-position-marker")!!.iconRotate.value!!
                                maxHeadingGap=maxOf(maxHeadingGap,abs(headingDelta(marker,map.cameraPosition.bearing.toFloat())))
                            }
                        }
                        result.put(JSONObject().put("sensorUpdates",noisy).put("step",step)
                            .put("fraction700ms",at700).put("fraction950ms",at950).put("maxHeadingGapDegrees",maxHeadingGap))
                    }
                }
            } finally { scenario.onActivity { view.destroy() } }
        }
        File(inst.targetContext.getExternalFilesDir(null),"car-motion-${if(baseline)"before" else "after"}.json").writeText(result.toString(2))
        File(inst.targetContext.getExternalFilesDir(null),"car-render-fps-${if(baseline)"before" else "after"}.json")
            .writeText(JSONArray(renderFps.toList()).toString(2))
        if(!baseline) for(index in 1 until result.length()) {
            val row=result.getJSONObject(index)
            assertTrue("Vehicle should still move late in each GPS interval: $row",row.getDouble("fraction950ms")-row.getDouble("fraction700ms")>.12)
            assertTrue("Repeated state emissions must not postpone arrival: $row",row.getDouble("fraction950ms")>.82)
            assertTrue("Vehicle and following camera must turn together: $row",row.getDouble("maxHeadingGapDegrees")<2.0)
        }
    }
}
