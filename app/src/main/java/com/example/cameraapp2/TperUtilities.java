package com.example.cameraapp2;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TperUtilities {
    private static final String TAG = TperUtilities.class.getSimpleName();
    private final Map<Integer, String> busStopDictionary;
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

        try {
            Resources res = context.getResources();
            InputStream in = res.openRawResource(R.raw.fermate);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);
            //we skip the first line
            br.readLine();
            String actualLine;
            while ((actualLine = br.readLine()) != null) {
                String[] data = actualLine.split(context.getString(R.string.tper_dictionary_separator));
                Integer stopCode = Integer.valueOf(data[0]);
                String stopName = data[1];
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
}
