package com.waypoint.app

import androidx.compose.ui.unit.sp

/**
 * Smallest sizes the app uses on screen. Nothing readable sits below Caption (12 sp), so small captions,
 * compass degrees, legend text and unit labels all follow the system font scale at the same floor.
 */
internal object TypeScale {
    val Caption = 12.sp
    val Label = 14.sp
    val Body = 16.sp
}
