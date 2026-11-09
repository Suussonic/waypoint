package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CursorModelTest {
    private fun glb(extra: String=""): ByteArray {
        val json="""{"asset":{"version":"2.0"},"meshes":[{}]$extra}"""
        val padded=json.padEnd((json.length+3)/4*4,' ').toByteArray()
        return ByteBuffer.allocate(36+padded.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0x46546c67);putInt(2);putInt(capacity())
            putInt(padded.size);putInt(0x4e4f534a);put(padded)
            putInt(8);putInt(0x004e4942);putLong(0)
        }.array()
    }
    @Test fun listsOnlyBundledGlbFileNames() {
        assertEquals(listOf("Boat.GLB","Minecart.glb"),
            cursorModelFileNames(listOf(".gitkeep","readme.md","Minecart.glb","Boat.GLB","folder/car.glb","..\\car.glb")))
    }
    @Test fun hidesOnlyTheExtension() {
        assertEquals("Minecart",cursorModelDisplayName("Minecart.glb"))
        assertEquals("Minecart.blue",cursorModelDisplayName("Minecart.blue.GLB"))
    }
    @Test fun acceptsSelfContainedGlb() { validateCursorGlb(glb()) }
    @Test fun rejectsExternalResources() {
        for(extra in listOf(""","images":[{"uri":"texture.png"}]""",""","buffers":[{"uri":"mesh.bin"}]""")) {
            assertThrows(IllegalArgumentException::class.java) { validateCursorGlb(glb(extra)) }
        }
    }
    @Test fun rejectsInvalidHeaderAndOversizedFiles() {
        val bytes=glb()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4,1)
        assertThrows(IllegalArgumentException::class.java) { validateCursorGlb(bytes) }
        assertThrows(IllegalArgumentException::class.java) { validateCursorGlb(ByteArray(MAX_CURSOR_MODEL_BYTES+1)) }
    }
    @Test fun tiltRefreshesOnlyInFiveDegreeSteps() {
        assertEquals(45f,cursorModelTilt(44.0))
        assertEquals(45f,cursorModelTilt(46.9))
        assertEquals(50f,cursorModelTilt(47.6))
        assertEquals(0f,cursorModelTilt(-3.0))
        assertEquals(90f,cursorModelTilt(120.0))
    }
    @Test fun rejectsTruncatedBinary() {
        val bytes=glb().dropLast(4).toByteArray()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(8,bytes.size)
        assertThrows(IllegalArgumentException::class.java) { validateCursorGlb(bytes) }
    }
}
