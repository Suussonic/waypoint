package com.waypoint.app

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/** Optional device geocoder. Its backend depends on the Android device, not on OSM. */
class AndroidPlaceSearch(context:Context):PlaceSearchProvider {
    private val geocoder=Geocoder(context.applicationContext,Locale.getDefault())
    override suspend fun find(query:String,near:Coordinate):List<Place> {
        require(query.length in 2..256 && near.isValid())
        if(!Geocoder.isPresent())throw IOException("Device search unavailable")
        val south=(near.lat-.25).coerceAtLeast(-90.0);val north=(near.lat+.25).coerceAtMost(90.0)
        val west=(near.lon-.3).coerceAtLeast(-180.0);val east=(near.lon+.3).coerceAtMost(180.0)
        val addresses=if(Build.VERSION.SDK_INT>=33)suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(query,8,south,west,north,east,object:Geocoder.GeocodeListener {
                override fun onGeocode(addresses:MutableList<Address>) {
                    if(continuation.isActive)continuation.resumeWith(Result.success(addresses.take(8)))
                }
                override fun onError(errorMessage:String?) {
                    if(continuation.isActive)continuation.resumeWith(Result.failure(IOException("Device search unavailable")))
                }
            })
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query,8,south,west,north,east).orEmpty()
        }
        return addresses.take(8).mapNotNull { address ->
            if(!address.hasLatitude() || !address.hasLongitude())return@mapNotNull null
            val point=Coordinate(address.latitude,address.longitude)
            if(!point.isValid())return@mapNotNull null
            fun clean(value:String?)=value.orEmpty().take(256).map{if(it.isISOControl())' ' else it}.joinToString("").trim()
            val feature=clean(address.featureName)
            // Some backends return only a Plus Code for a named junction. Keep the queried
            // text as an explicit address-result label, not a verified landmark name.
            val addressOnly=Regex("[23456789CFGHJMPQRVWX]{2,8}\\+[23456789CFGHJMPQRVWX]{2,}",RegexOption.IGNORE_CASE).matches(feature)
            val name=if(addressOnly)"Address result for $query" else feature.ifBlank{clean(address.thoroughfare)}.ifBlank{clean(address.locality)}
            if(name.isBlank())return@mapNotNull null
            val label=clean(address.getAddressLine(0))
            Place("android:${point.lat},${point.lon}:$name",name,label,point,
                if(name==address.locality || name==address.adminArea || name==address.countryName)"locality" else "geocoder", if(addressOnly)"Android geocoder · address match" else "Android geocoder")
        }
    }
}
