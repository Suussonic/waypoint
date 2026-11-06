package com.waypoint.app
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.security.MessageDigest
class WorldCatalogueTest {
    @Test fun everyThemeHasAValidStyleAndNoDuplicateLayers() {
        for(world in World.entries) {
            val file=File("src/main/assets/styles/${world.file}.json")
            assertTrue("Missing ${world.title}",file.isFile)
            val style=JSONObject(file.readText());val layers=style.getJSONArray("layers");val ids=mutableSetOf<String>()
            for(i in 0 until layers.length()) {
                val layer=layers.getJSONObject(i)
                assertTrue("Duplicate layer in ${world.title}",ids.add(layer.getString("id")))
                if(layer.has("source"))assertTrue(style.getJSONObject("sources").has(layer.getString("source")))
            }
            assertEquals("Terrain flag ${world.title}",world.hasTerrain(),style.getJSONObject("sources").has("waypoint-terrain"))
            assertEquals("Authored style ${world.title}",true,style.getJSONObject("metadata").getBoolean("waypoint:authored"))
            assertFalse("No preset sprite ${world.title}",style.has("sprite"))
        }
    }
    @Test fun everyWorldShipsDayAndNightArtAndStyles() {
        fun digest(file:File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val dayAtlases=mutableSetOf<String>()
        val nightAtlases=mutableSetOf<String>()
        fun pngSize(file:File):Pair<Int,Int> {
            DataInputStream(file.inputStream()).use { stream ->
                assertEquals("PNG signature ${file.name}",0x89504E470D0A1A0AuL.toLong(),stream.readLong())
                assertEquals("IHDR length ${file.name}",13,stream.readInt())
                assertEquals("IHDR marker ${file.name}",0x49484452,stream.readInt())
                return stream.readInt() to stream.readInt()
            }
        }
        for(world in World.entries) {
            for(phase in listOf("day","night")) {
                assertEquals("${world.name} $phase material",128 to 128,pngSize(File("src/main/assets/map-materials/${world.file}_$phase.png")))
                val suffix=if(phase=="night")"_night" else ""
                val atlas=File("src/main/assets/poi/${world.file}$suffix.png")
                assertEquals("${world.name} $phase icon atlas",640 to 1280,pngSize(atlas))
                assertTrue("${world.name} $phase atlas has a unique symbol grammar",
                    (if(phase=="night")nightAtlases else dayAtlases).add(digest(atlas)))
                assertEquals("${world.name} $phase destination marker",128 to 128,pngSize(File("src/main/assets/map-markers/${world.file}$suffix.png")))
            }
            assertTrue("Missing ${world.name} night style",File("src/main/assets/styles/${world.file}_night.json").isFile)
        }
        assertEquals("Unique day POI atlases",World.entries.size,dayAtlases.size)
        assertEquals("Unique night POI atlases",World.entries.size,nightAtlases.size)
        for(name in listOf("division_holo","division_holo_night"))
            assertTrue("Missing Dark Zone style $name",File("src/main/assets/styles/$name.json").isFile)
    }
    @Test fun roadCameraExpressionsRemainValidForNativeRendering() {
        fun hasZoom(value:Any?):Boolean = value is JSONArray && (value.optString(0)=="zoom" || (0 until value.length()).any{hasZoom(value.opt(it))})
        for(world in World.entries) {
            val layers=JSONObject(File("src/main/assets/styles/${world.file}.json").readText()).getJSONArray("layers")
            for(i in 0 until layers.length()) {
                val layer=layers.getJSONObject(i)
                if(layer.optString("source-layer")!="transportation" || layer.optString("type")!="line")continue
                val width=layer.optJSONObject("paint")?.opt("line-width")
                if(!hasZoom(width))continue
                assertTrue("${world.name}/${layer.getString("id")}: zoom must be top-level",width is JSONArray)
                val expr=width as JSONArray
                val op=expr.optString(0)
                assertTrue("${world.name}/${layer.getString("id")}: invalid camera expression $op",op in listOf("interpolate","step"))
                val input=if(op=="step")1 else 2
                assertEquals("zoom",expr.getJSONArray(input).getString(0))
                for(n in 1 until expr.length())if(n!=input)assertFalse("Nested zoom expression",hasZoom(expr.opt(n)))
            }
        }
    }
    @Test fun everyThemeKeepsConnectedRoadClasses() {
        for(world in World.entries) {
            val layers=JSONObject(File("src/main/assets/styles/${world.file}.json").readText()).getJSONArray("layers")
            for(id in listOf("waypoint-primary-roads","waypoint-secondary-roads","waypoint-local-roads","waypoint-path-roads")) {
                val road=(0 until layers.length()).map{layers.getJSONObject(it)}.first{it.getString("id")==id}
                assertNotEquals("${world.name}/$id is hidden","none",road.optJSONObject("layout")?.optString("visibility"))
                assertTrue(road.getJSONObject("paint").has("line-width"))
            }
        }
    }
    @Test fun gameAbbreviationsFindTheirHud(){assertTrue(World.LOS_SANTOS.matchesGameQuery("GTA V"));assertTrue(World.OLD_WEST.matchesGameQuery("rdr2"));assertTrue(World.HYRULE.matchesGameQuery("BOTW"))}
    @Test fun punctuationDoesNotBreakGameSearch(){assertTrue(World.ASSASSIN.matchesGameQuery("Assassin's Creed"));assertTrue(World.SPIDER.matchesGameQuery("Spider Man"))}
    @Test fun unrelatedNamesDoNotMatch(){assertFalse(World.SKYRIM.matchesGameQuery("Forza"));assertTrue(World.SKYRIM.matchesGameQuery("  "))}

    @Test fun onboardingCarouselIncludesEveryWorldAndLeadsWithFavorites() {
        val favorites = listOf(World.PIXEL, World.OLD_WEST)
        val carousel = onboardingCarouselWorlds(favorites)
        assertEquals(favorites, carousel.take(favorites.size))
        assertEquals(World.entries.size, carousel.size)
        assertEquals(World.entries.toSet(), carousel.toSet())
    }

    @Test fun rapidFavoriteTogglesAccumulateAndOnlyRemoveTheTappedWorld() {
        var names = ""
        names = toggleFavoriteName(names, World.OLD_WEST)
        names = toggleFavoriteName(names, World.PIXEL)
        names = toggleFavoriteName(names, World.RIVIA)
        assertEquals("OLD_WEST,PIXEL,RIVIA", names)
        names = toggleFavoriteName(names, World.PIXEL)
        assertEquals("OLD_WEST,RIVIA", names)
        assertEquals("", toggleFavoriteName("RIVIA", World.RIVIA))
    }

    @Test fun onboardingFavoriteChoicesCoverEveryWorldOnce() {
        assertEquals(World.entries.size, gameChoices.size)
        assertEquals(World.entries.toSet(), gameChoices.map { it.world }.toSet())
    }

    @Test fun everyWorldHasAFullSizeGlobeTexture() {
        for (world in World.entries) {
            val file = File("src/main/assets/globe/textures/${world.file}.png")
            assertTrue("Missing globe texture for ${world.title}", file.isFile)
            DataInputStream(file.inputStream()).use { stream ->
                assertEquals("PNG signature for ${world.title}", 0x89504E470D0A1A0AuL.toLong(), stream.readLong())
                assertEquals("IHDR length for ${world.title}", 13, stream.readInt())
                assertEquals("IHDR marker for ${world.title}", 0x49484452, stream.readInt())
                assertEquals("Width for ${world.title}", 4096, stream.readInt())
                assertEquals("Height for ${world.title}", 2048, stream.readInt())
            }
        }
    }
}
