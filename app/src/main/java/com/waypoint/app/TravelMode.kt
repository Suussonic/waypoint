package com.waypoint.app

/** Add a mode here with an actual routing profile, rather than reusing car ETAs. */
enum class TravelMode(val label:String,val costing:String,val routeDescription:String) {
    CAR("Car","auto","driving"),
    MOTORCYCLE("Motorcycle","motorcycle","motorcycle"),
    BICYCLE("Bicycle","bicycle","cycling"),
    WALK("Walk","pedestrian","walking")
}
