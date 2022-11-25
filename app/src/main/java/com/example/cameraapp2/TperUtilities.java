package com.example.cameraapp2;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TperUtilities {
    private static final String TAG = TperUtilities.class.getSimpleName();
    private final Map<Integer, String> busStopDictionary;
    private Map<Integer, GeoPoint> coordinates;
    Context context;

    public TperUtilities(Context context) {
        this.context = context;
        busStopDictionary = loadData(context);

    }

    public String getBusStopByCode(int code) {
        String stopName = busStopDictionary.get(code);
        if (stopName == null) {
            return context.getString(R.string.non_existent_bus_stop);
        }
        return stopName;
    }

    public GeoPoint getGeoPointByCode(int code) {
        return coordinates.get(code);
    }

    public List<GeoPoint> getCoupleOfCoordinatesByStopName(String stopName) {
        List<Integer> codes = getCodesByStopName(getMoreSimilarBusStop(stopName));
        List<GeoPoint> points = new ArrayList<>();

        for (int code : codes) {
            points.add(getGeoPointByCode(code));
        }
        return points;
    }

    public Boolean codeIsBusStop(String busStopCodeString) {
        try {
            int code = Integer.parseInt(busStopCodeString);
            return busStopDictionary.containsKey(code);
        } catch (NumberFormatException e) {
            Log.e(TAG, "EXCEPTION: " + e);
        }
        return false;
    }

    private Boolean isBusStop(String aPossibleBusStop) {
        return busStopDictionary.containsValue(aPossibleBusStop);
    }

    private static String findClosestMatch(Collection<String> collection, String target) {
        int currMinDistance = Integer.MAX_VALUE;
        String closest = null;

        for (String compareObject : collection) {
            int currentDistance = StringUtils.getLevenshteinDistance(compareObject, target);
            if (currentDistance < currMinDistance) {
                currMinDistance = currentDistance;
                closest = compareObject;
            }
        }

        return closest;
    }

    public String getMoreSimilarBusStop(String aPossibleBusStop) {
        Log.d(TAG, "Received:" + aPossibleBusStop);
        if (Boolean.TRUE.equals(isBusStop(aPossibleBusStop))) {
            Log.wtf(TAG, "it's a valid name for a stop");
            return aPossibleBusStop;
        } else {
            Log.wtf(TAG, "must look for similarity");
            return String.valueOf(findClosestMatch(busStopDictionary.values(), aPossibleBusStop));
        }
    }

    public Map<Integer, String> loadData(Context context) {
        Map<Integer, String> busStopList = new HashMap<>();
        coordinates = new HashMap<>();

        try {
            Resources res = context.getResources();
            InputStream in = res.openRawResource(R.raw.fermate);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);

            br.readLine(); //skip the first line
            String actualLine;
            while ((actualLine = br.readLine()) != null) {
                String[] data = actualLine.split(context.getString(R.string.tper_dictionary_separator));
                Integer stopCode = Integer.valueOf(data[0]);
                String stopName = data[1];
                double latitude = Double.parseDouble(data[6].replace(",", "."));
                double longitude = Double.parseDouble(data[7].replace(",", "."));
                GeoPoint stopGeoPoint = new GeoPoint(latitude, longitude);
                coordinates.put(stopCode, stopGeoPoint);
                busStopList.put(stopCode, stopName);
            }

            br.close();
            isr.close();
            in.close();

            return busStopList;

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
        return Collections.emptyMap();
    }

    public List<Integer> getCodesByStopName(String stopName) {
        List<Integer> codes = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : busStopDictionary.entrySet()) {
            if (stopName.equals(entry.getValue())) {
                codes.add(entry.getKey());
            }
        }
        return codes;
    }
}
