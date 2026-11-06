package com.waypoint.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

fun interface PlaceSearchProvider { suspend fun find(query:String, near:Coordinate):List<Place> }

/** Names are compared locally; approximate matches retain the provider's actual name and point. */
object PlaceMatcher {
    fun normalize(text:String):String = Normalizer.normalize(text.take(256),Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    private val generic=setOf("railway","railroad","train","station","stn","rly","restaurant","hotel","near","in")
    private fun core(text:String)=normalize(text).split(' ').filter{it !in generic && it.isNotBlank()}.joinToString(" ")
    private fun similarity(a:String,b:String):Double {
        if(a==b)return 1.0
        if(a.isEmpty() || b.isEmpty())return 0.0
        if(b.startsWith(a) && a.length>=3)return .92
        val previous=IntArray(b.length+1){it};val current=IntArray(b.length+1)
        for(i in a.indices) {
            current[0]=i+1
            for(j in b.indices)current[j+1]=minOf(current[j]+1,previous[j+1]+1,previous[j]+if(a[i]==b[j])0 else 1)
            current.copyInto(previous)
        }
        return 1.0-previous[b.length].toDouble()/maxOf(a.length,b.length)
    }
    fun score(query:String,place:Place):Double {
        val normalized=normalize(query)
        val categories=when(normalized) {
            "food", "restaurant", "restaurants" -> setOf("restaurant","fast_food","food_court","food")
            "coffee", "cafe", "cafes" -> setOf("cafe","coffee_shop")
            "fuel", "petrol", "petrol station", "gas station" -> setOf("fuel")
            "parking" -> setOf("parking")
            "hotel", "hotels" -> setOf("hotel","motel","hostel","guest_house")
            else -> emptySet()
        }
        if(place.kind in categories)return 1.0
        val q=core(query).ifBlank{normalized}
        val name=core(place.name).ifBlank{normalize(place.name)}
        val compact=similarity(q.replace(" ",""),name.replace(" ",""))
        val tokens=q.split(' ')
        val candidate=(name+" "+normalize(place.address)).split(' ').filter{it.isNotEmpty()}
        val tokenScore=tokens.map { token -> candidate.maxOfOrNull{similarity(token,it)} ?: 0.0 }.average()
        var score=maxOf(compact,tokenScore*.96)
        val wantsStation=normalize(query).split(' ').any{it in setOf("railway","railroad","train","station","stn","rly")}
        if(wantsStation && place.kind !in setOf("station","halt","train_station","geocoder") && !normalize(place.name).contains("station"))score*=.35
        return score
    }
    fun ranked(query:String,near:Coordinate,places:List<Place>):List<Place> {
        val scored=places.filter{it.point.isValid()}.map{it to score(query,it)}
        val best=scored.maxOfOrNull{it.second} ?: 0.0
        // Once a strong match exists, don't pad the list with similarly spelled places
        // on another continent. Weak-only results remain available as suggestions.
        val cutoff=if(best>=.88)maxOf(.52,best-.12) else .52
        return scored.filter{it.second>=cutoff}
            .sortedWith(compareByDescending<Pair<Place,Double>>{it.second}.thenBy{distance(near,it.first.point)})
            .map{it.first}.fold(mutableListOf<Place>()) { out,p ->
                if(out.none{it.id==p.id || normalize(it.name)==normalize(p.name) && distance(it.point,p.point)<100})out.add(p)
                out
            }.take(12)
    }
    fun variants(query:String):List<String> {
        val cleaned=query.trim().replace(Regex("\\s+")," ")
        // Common concatenated junction suffixes; no guessed coordinates or silently invented places.
        val spaced=cleaned.replace(Regex("(?i)([\\p{L}]{4,})(more|morh|chowk|junction)$"),"$1 $2")
        return listOf(cleaned,spaced).distinct()
    }
}

/** Device-local confirmed spellings. No personal queries are bundled in the app. */
class SearchAliases(entries:Map<String,String> = emptyMap()) {
    private val aliases=entries.entries.take(100).filter {
        it.key.isNotBlank() && it.value.isNotBlank() && it.key.length<=256 && it.value.length<=256 &&
            it.key.none(Char::isISOControl) && it.value.none(Char::isISOControl)
    }.associate { PlaceMatcher.normalize(it.key) to it.value.trim() }
    fun resolve(query:String):String = aliases[PlaceMatcher.normalize(query)] ?: query
}

data class PlaceSearchResult(val places:List<Place>,val note:String?=null)

/** Coordinate pairs are destinations, not names to send to the geocoder. */
internal fun coordinateSearchResult(query: String, local: List<Place>): PlaceSearchResult? {
    val value = query.trim()
    if (!Regex("^[+-]?[0-9.]+\\s*,\\s*[+-]?[0-9.]*\\s*(?:\\([^()]*\\))?$").matches(value)) return null
    val encoded = URLEncoder.encode(value, "UTF-8")
    val point = (NavigationImport.parse("android.intent.action.VIEW", "geo:0,0?q=$encoded") as? NavigationImport.Point)
        ?.coordinate ?: return PlaceSearchResult(emptyList(), "Enter valid coordinates as latitude, longitude (for example, 51.5007, -0.1246).")
    val saved = local.firstOrNull { it.point == point }
    val place = saved ?: Place("coordinate:${point.lat},${point.lon}", coordinateLabel(point),
        "Coordinates: ${point.lat}, ${point.lon}", point, "coordinates")
    return PlaceSearchResult(listOf(place))
}

/** Bounded, memory-only cache and two independent providers; no remote autocomplete fan-out. */
class PlaceSearch(private val primary:PlaceSearchProvider,private val fallback:PlaceSearchProvider?=null,private val aliases:SearchAliases=SearchAliases()) {
    private val cache=LinkedHashMap<String,List<Place>>()
    private val mutex=Mutex()
    suspend fun search(query:String,near:Coordinate,local:List<Place> = emptyList(),expanded:Boolean=false):PlaceSearchResult = withContext(Dispatchers.Default) { mutex.withLock {
        require(query.length<=256){"Use a shorter place name or address (up to 256 characters)."}
        require(near.isValid()){"Waiting for a valid location."}
        coordinateSearchResult(query, local)?.let { return@withLock it }
        val effectiveQuery=aliases.resolve(query)
        val aliasNote=if(effectiveQuery!=query)"Searching for $effectiveQuery" else null
        val variants=PlaceMatcher.variants(effectiveQuery)
        val key="${PlaceMatcher.normalize(query)}:${(near.lat*100).roundToInt()}:${(near.lon*100).roundToInt()}:$expanded"
        cache[key]?.let{return@withLock PlaceSearchResult(PlaceMatcher.ranked(effectiveQuery,near,it+local),aliasNote)}
        val candidates=local.toMutableList()
        val remote=mutableListOf<Place>()
        var failures=0
        var completed=0
        suspend fun lookup(provider:PlaceSearchProvider,q:String) {
            try {
                val results=withTimeoutOrNull(9000){provider.find(q,near)}
                if(results==null)failures++ else {
                    val bounded=results.take(20)
                    remote.addAll(bounded);candidates.addAll(bounded);completed++
                }
            } catch(e:CancellationException){throw e} catch(_:Exception){failures++}
        }
        lookup(primary,variants.first())
        val primaryMatches=PlaceMatcher.ranked(effectiveQuery,near,candidates)
        if(fallback!=null && (expanded || primaryMatches.none{PlaceMatcher.score(effectiveQuery,it)>=.88})) {
            lookup(fallback,variants.first())
            if(variants.size>1 && PlaceMatcher.ranked(effectiveQuery,near,candidates).isEmpty())lookup(fallback,variants.last())
        }
        val results=PlaceMatcher.ranked(effectiveQuery,near,candidates)
        // Saved/recent matches are supplied afresh: deleting one must not leave a cached copy.
        if(failures==0) { cache[key]=remote;while(cache.size>40)cache.remove(cache.keys.first()) }
        val note=when {
            failures>0 && results.isEmpty() -> "Search service unavailable. Check your connection and try again."
            failures>0 -> "Some search services are unavailable. Showing the matches found."
            results.isEmpty() && completed>0 -> "No matching place found. Try an alternate spelling, add the town, or drop a pin on the map."
            results.none{PlaceMatcher.score(effectiveQuery,it)>=.8} -> "Possible matches · check the name and address before choosing."
            else -> aliasNote
        }
        PlaceSearchResult(results,note)
    } }
}
