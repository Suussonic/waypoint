package com.waypoint.app

import org.junit.Assert.*
import org.junit.Test

class UpdateAlertRulesTest {
    @Test fun onlyNewUnannouncedVersionsCanNotify() {
        assertTrue(UpdateAlerts.shouldAnnounce(2000001,2000000,0,true,false))
        assertFalse(UpdateAlerts.shouldAnnounce(2000000,2000000,0,true,false))
        assertFalse(UpdateAlerts.shouldAnnounce(1005000,2000000,0,true,false))
        assertFalse(UpdateAlerts.shouldAnnounce(2000001,2000000,2000001,true,false))
        assertFalse(UpdateAlerts.shouldAnnounce(2000001,2000000,2000002,true,false))
    }
    @Test fun deniedPermissionsAndDrivingSuppressDelivery() {
        assertFalse(UpdateAlerts.shouldAnnounce(2000001,2000000,0,false,false))
        assertFalse(UpdateAlerts.shouldAnnounce(2000001,2000000,0,true,true))
    }
}
