package com.example.cameraapp2.tper

import android.content.Context
import android.util.Log
import com.example.cameraapp2.R
import org.apache.commons.text.similarity.LevenshteinDistance
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Utility class for TPER data.
 *
 * @constructor
 * Takes a [Context] as a parameter, used to load the TPER stops data from file.
 * @param context The context of the application.
 */
class TperUtilities(context: Context) {
    private var busStopDictionary: MutableMap<Int, String> = mutableMapOf()
    private var coordinates: MutableMap<Int, GeoPoint> = mutableMapOf()

    init {
        loadData(context)
    }

    // guarda perché è diversa ora
    /**
     * Returns the bus stop name from its code number
     *
     * @param code the code number of the bus stop
     * @return the name of the bus stop, or an empty string if the code is not found
     */
    fun getBusStopByCode(code: Int): String {
        return busStopDictionary[code] ?: return ""
    }

    fun getMoreSimilarBusStop(aPossibleBusStop: String): String {
        Log.d(TAG, "Received:$aPossibleBusStop")
        return if (java.lang.Boolean.TRUE == isBusStop(aPossibleBusStop)) {
            Log.wtf(TAG, "it's a valid name for a stop")
            aPossibleBusStop
        } else {
            Log.wtf(TAG, "must look for similarity")
            findClosestMatch(busStopDictionary.values, aPossibleBusStop).toString()
        }
    }

    fun getGeoPointByCode(code: Int): GeoPoint? {
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

    fun codeIsBusStop(busStopCodeString: String): Boolean {
        try {
            val code = busStopCodeString.toInt()
            return busStopDictionary.containsKey(code)
        } catch (e: NumberFormatException) {
            Log.e(TAG, "EXCEPTION: $e")
        }
        return false
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

    private fun isBusStop(aPossibleBusStop: String): Boolean {
        return busStopDictionary.containsValue(aPossibleBusStop)
    }

    private fun findClosestMatch(collection: Collection<String>, target: String): String? {
        var currMinDistance = Int.MAX_VALUE
        var closest: String? = null
        for (compareObject in collection) {
            val currentDistance =
                LevenshteinDistance.getDefaultInstance().apply(compareObject, target)
            Log.d(
                TAG, "comparing: $compareObject with $target distance: $currentDistance"
            )
            if (currentDistance < currMinDistance) {
                currMinDistance = currentDistance
                closest = compareObject
            }
        }
        Log.i(TAG, "Closest match: $closest")
        Log.i(
            TAG,
            "Distance: $currMinDistance; max allowed: ${maxLevenshteinDistanceFromTarget(target)}"
        )
        if (currMinDistance > maxLevenshteinDistanceFromTarget(target)) closest = null
        return closest
    }

    private fun loadData(context: Context) {
        coordinates = mutableMapOf()
        try {
            val res = context.resources
            val inputStream = res.openRawResource(R.raw.fermate)
            val isr = InputStreamReader(inputStream)
            val br = BufferedReader(isr)
            br.readLine() //skip the first line
            var line: String
            while (br.readLine().also { line = it } != null) {
                val data =
                    line.split(context.getString(R.string.tper_dictionary_separator)).toTypedArray()
                val stopCode = Integer.valueOf(data[0])
                val stopName = data[1]
                val latitude = data[6].replace(",", ".").toDouble()
                val longitude = data[7].replace(",", ".").toDouble()
                val stopGeoPoint = GeoPoint(latitude, longitude)
                coordinates[stopCode] = stopGeoPoint
                busStopDictionary[stopCode] = stopName
            }
            br.close()
            isr.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
    }

    companion object {
        private val TAG = TperUtilities::class.java.simpleName
        private const val LEVENSHTEIN_MAX_DISTANCE_RATIO = 0.6

        fun maxLevenshteinDistanceFromTarget(target: String): Int {
            return (target.length * LEVENSHTEIN_MAX_DISTANCE_RATIO).toInt()
        }
    }
}