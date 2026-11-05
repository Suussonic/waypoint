package com.waypoint.app

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only checks of the installed package's security configuration. */
@RunWith(AndroidJUnit4::class)
class SecurityConfigurationTest {
    @Suppress("DEPRECATION")
    @Test fun carServiceIsDiscoverableButOtherServicesStayPrivate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = context.packageManager.getServiceInfo(
            ComponentName(context.packageName,"com.waypoint.app.WaypointCarService"),
            0)
        assertTrue("Android Auto must be able to discover Waypoint", service.enabled)
        assertTrue("Android Auto host must be able to bind", service.exported)
    }

    @Suppress("DEPRECATION")
    @Test fun privateStorageAndNetworkPolicyArePackaged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_PROVIDERS or PackageManager.GET_SERVICES)
        val provider = requireNotNull(info.providers).single { it.authority == "${context.packageName}.updates" }
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        val services = requireNotNull(info.services)
        assertEquals(
            setOf("com.waypoint.app.WaypointCarService", "com.waypoint.app.UpdateCheckJob"),
            services.filter { it.exported }.map { it.name }.toSet()
        )
        val updateJob = services.single { it.name == "com.waypoint.app.UpdateCheckJob" }
        assertEquals("android.permission.BIND_JOB_SERVICE", updateJob.permission)
        assertEquals(0,context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(0,context.applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)
    }

    @Suppress("DEPRECATION")
    @Test fun debugWorldPreviewCannotBeStartedByOtherApps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = ComponentName(context.packageName, "com.waypoint.app.WorldPreviewCaptureActivity")
        val preview = try {
            context.packageManager.getActivityInfo(component, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        if (BuildConfig.DEBUG) {
            assertNotNull("Debug preview should be packaged in debug builds", preview)
            assertFalse("Preview capture must be private to Waypoint", preview!!.exported)
        } else {
            assertNull("Debug preview must be absent from release builds", preview)
        }
    }
}
