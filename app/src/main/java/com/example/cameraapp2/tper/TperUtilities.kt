package com.example.cameraapp2.tper

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp2.viewmodel.BusStopViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.text.similarity.LevenshteinDistance
import org.osmdroid.util.GeoPoint
import kotlin.math.floor

/**
 * Utility class for TPER data.
 *
 * @constructor
 * Takes a [Context] as a parameter, used to load the TPER stops data from file.
 *
 * @param context The context of the application.
 */
class TperUtilities(context: Context) {
    private val busStopViewModel: BusStopViewModel
    private var busStopDictionary: MutableMap<Int, String> = mutableMapOf()
    private var coordinates: MutableMap<Int, GeoPoint> = mutableMapOf()

    init {
        busStopViewModel =
            ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                .create(BusStopViewModel::class.java)

        // Load data from db
        val data = busStopViewModel.readAllData

        CoroutineScope(Dispatchers.Default).launch {
            data.forEach { busStop ->
                busStopDictionary[busStop.code] = busStop.name
                coordinates[busStop.code] = GeoPoint(busStop.latitude, busStop.longitude)
            }
        }
    }

    /**
     * Returns the bus stop name from its code number
     *
     * @param code the code number of the bus stop
     * @return the name of the bus stop, or an empty string if the code is not found
     */
    fun getBusStopByCode(code: Int): String {
        return busStopDictionary[code] ?: return ""
    }

    /**
     * Returns the most similar bus stop name from a given string.
     *
     * @param aPossibleBusStop name of a possible bus stop
     * @return the name of the most similar bus stop, or an empty string if no similar bus stop is found
     */
    fun getMoreSimilarBusStop(aPossibleBusStop: String): String = when {
        aPossibleBusStop.isEmpty() -> ""
        busStopDictionary.containsValue(aPossibleBusStop) -> aPossibleBusStop
        else -> {
            Log.d(TAG, "must look for similarity")

            findClosestMatch(aPossibleBusStop) ?: ""
        }
    }

    private fun getGeoPointByCode(code: Int): GeoPoint? {
        return coordinates[code]
    }

    fun getCoupleOfCoordinatesByStopName(stopName: String): List<GeoPoint> {
        val codes = getCodesByStopName(getMoreSimilarBusStop(stopName))
        val points: MutableList<GeoPoint> = mutableListOf()
        var point: GeoPoint?
        for (code in codes) {
            point = getGeoPointByCode(code)
            if (point != null) points.add(point)
        }
        return points
    }

    fun getMostSimilarStopCode(aPossibleBusCode: String): String {
        val processedCode: String =
            NumbersInferencePostProcessing.substitutionStep(aPossibleBusCode)
        return Companion.findClosestMatch(
            busStopDictionary.keys.map { it.toString() },
            processedCode,
            LEVENSHTEIN_MAX_DISTANCE_FOR_STOP_CODE_SEARCH
        ) ?: ""
    }

    fun getCodesByStopName(stopName: String): List<Int> {
        Log.d(
            TAG, "getCodesByStopName - Received:$stopName"
        )
        val codes: MutableList<Int> = ArrayList()
        for ((key, value) in busStopDictionary) {
            if (stopName == value) {
                codes.add(key)
            }
        }
        Log.d(
            TAG, "getCodesByStopName - Found:$codes"
        )

        return codes
    }

    /**
     * Returns the most similar bus stop name to [aPossibleBusStop] using the Levenshtein distance.
     *
     * [aPossibleBusStop] is compared in uppercase, since the bus stop names are all uppercase,
     * and we don't want to consider the case in the Levenshtein distance.
     *
     * @param aPossibleBusStop
     * @return the most similar bus stop name, or null if no similar bus stop is found
     */
    private fun findClosestMatch(aPossibleBusStop: String): String? {
        return Companion.findClosestMatch(
            busStopDictionary.values,
            aPossibleBusStop.uppercase(),
            maxLevenshteinDistanceFromTarget(aPossibleBusStop)
        )
    }

    companion object {
        private val TAG = TperUtilities::class.java.simpleName

        /**
         * Levenshtein Max Distance Ratio, i.e. the max ratio between the Levenshtein distance and
         * the length of the target string.
         */
        private const val LEVENSHTEIN_MAX_DISTANCE_RATIO = 0.6

        /**
         * Maximum Levenshtein distance for a possible stop code from an actual stop code to be
         * considered a match.
         */
        private const val LEVENSHTEIN_MAX_DISTANCE_FOR_STOP_CODE_SEARCH = 1

        /**
         * Returns the maximum Levenshtein from [target] allowed for a string to be considered a
         * valid match.
         *
         * @param target The target string.
         * @return The maximum Levenshtein distance from [target].
         */
        private fun maxLevenshteinDistanceFromTarget(target: String): Int {
            return floor(target.length * LEVENSHTEIN_MAX_DISTANCE_RATIO).toInt()
        }

        /**
         * Searches for the closest match of a string to be searched in a list of strings.
         * It uses the Levenshtein distance algorithm, with a maximum distance obtained by
         * [maxLevenshteinDistanceFromTarget].
         *
         * @param collection The collection in which to search for the closest match.
         * @param toSearch The target string.
         *
         * @return The closest match, i.e the string in [collection] that is the closest to
         * [toSearch], or null if no match is found.
         */
        private fun findClosestMatch(
            collection: Collection<String>, toSearch: String, maxDistance: Int
        ): String? {
            if (toSearch.isEmpty() or collection.isEmpty()) return null

            var currMinDistance = Int.MAX_VALUE
            var closest: String? = null
            for (compareObject in collection) {
                val currentDistance =
                    LevenshteinDistance.getDefaultInstance().apply(compareObject, toSearch)
                if (currentDistance < currMinDistance) {
                    currMinDistance = currentDistance
                    closest = compareObject
                }
            }
            Log.i(TAG, "Min distance found: $currMinDistance; max allowed: $maxDistance")

            if (currMinDistance > maxDistance) closest = null
            return closest
        }
    }
}