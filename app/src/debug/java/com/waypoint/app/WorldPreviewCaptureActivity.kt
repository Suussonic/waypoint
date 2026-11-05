package com.waypoint.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import java.io.FileOutputStream

/** Debug-only asset builder. Captures real MapLibre styles over their documented public-place fixtures. */
class WorldPreviewCaptureActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var snapshotter: MapSnapshotter? = null
    private lateinit var output: File
    private lateinit var status: TextView
    private var worlds: List<World> = emptyList()
    private val failures = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 18f; setPadding(40, 100, 40, 40) }
        setContentView(status)
        output = File(getExternalFilesDir(null), "world-previews").apply { mkdirs() }
        val only = intent.getStringExtra("only")
        worlds = if (only == null) World.entries else if (only == "new") newWorlds.toList() else World.entries.filter { it.file == only }
        require(worlds.isNotEmpty()) { "Unknown world: $only" }
        capture(0)
    }

    private fun capture(index: Int) {
        if (index == worlds.size) {
            File(output, "status.txt").writeText("Captured ${worlds.size - failures.size}/${worlds.size}\n" + failures.joinToString("\n"))
            status.text = "Done: ${worlds.size - failures.size}/${worlds.size}"
            return
        }
        val world = worlds[index]
        val place = world.previewPlace()
        status.text = "${index + 1}/${worlds.size}: ${world.title}\n${place.label}"
        val json = assets.open("styles/${world.file}.json").bufferedReader().use { it.readText() }
        val camera = CameraPosition.Builder()
            .target(LatLng(place.lat, place.lon)).zoom(place.zoom).tilt(if(world==World.NIER)42.0 else 0.0).bearing(0.0).build()
        val options = MapSnapshotter.Options(960, 520)
            .withStyleBuilder(Style.Builder().fromJson(json)).withCameraPosition(camera).withPixelRatio(1f).withLogo(false)
        val renderer = MapSnapshotter(this, options)
        snapshotter = renderer
        renderer.setObserver(object : MapSnapshotter.Observer {
            override fun onDidFinishLoadingStyle() = Unit
            override fun onStyleImageMissing(id: String) {
                val res = when (id) {
                    "pixel-grass" -> R.drawable.pixel_grass
                    "pixel-stone" -> R.drawable.pixel_stone
                    "pixel-water" -> R.drawable.pixel_water
                    "paper-fibre" -> R.drawable.paper_fibre
                    else -> return
                }
                renderer.addImage(id, BitmapFactory.decodeResource(resources, res), false)
            }
        })
        renderer.start({ result ->
            val file = File(output, "${world.file}.png")
            FileOutputStream(file).use { result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Log.i("WaypointPreview", "Captured ${world.file}: ${place.label} (${file.length()} bytes)")
            snapshotter = null
            handler.postDelayed({ capture(index + 1) }, 120)
        }, { error ->
            failures += "${world.file}: $error"
            Log.e("WaypointPreview", "Capture failed for ${world.file}: $error")
            snapshotter = null
            handler.postDelayed({ capture(index + 1) }, 120)
        })
    }

    override fun onDestroy() {
        snapshotter?.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
