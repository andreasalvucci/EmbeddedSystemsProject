package com.example.cameraapp2;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.opencsv.CSVReader;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TperUtilities {
    private Map<Integer,String> busStopDictionary;
    Context context;

    public TperUtilities(Context context){
        this.context=context;
        busStopDictionary = loadData(context);
    }

    public String getBusStopByCode(int code){
            String stopName = busStopDictionary.get(code);
            if(stopName==null){
                return "FERMATA NON ESISTENTE";
            }
            return stopName;
    }

    private Boolean isBusStop(String aPossibleBusStop){
        return busStopDictionary.containsValue(aPossibleBusStop);
    }

    private static Object findClosestMatch(Collection<String> collection, String target) {
        int distance = Integer.MAX_VALUE;
        Object closest = null;
        for (Object compareObject : collection) {
            int currentDistance = StringUtils.getLevenshteinDistance(compareObject.toString(), target.toString());
            if(currentDistance < distance) {
                distance = currentDistance;
                closest = compareObject;
            }
        }
        return closest;
    }

    public String getMoreSimilarBusStop(String aPossibleBusStop){
        Log.d("message", "Received:"+aPossibleBusStop);
        if(isBusStop(aPossibleBusStop)){
            Log.wtf("message","it's a valid name for a stop");
            return aPossibleBusStop;
        }

        else {
            Log.wtf("message", "must look for similarity");
            String candidateBusStop = String.valueOf(findClosestMatch(busStopDictionary.values(), aPossibleBusStop));
            return  candidateBusStop;

        }
    }

    public  Map<Integer,String> loadData(Context context){
        Map<Integer,String> busStopList = new HashMap<Integer,String>();
        try{
            Resources res = context.getResources();
            InputStream in = res.openRawResource(R.raw.fermate);
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);
            //we skip the first line
            String firstLine = br.readLine();
            String actualLine="";
            while((actualLine=br.readLine())!=null){
                String[] data = actualLine.split("\\;");
                Integer stopCode = Integer.valueOf(data[0]);
                String stopName = data[1];
                busStopList.put(stopCode,stopName);
            }
            br.close();
            isr.close();
            in.close();
            return busStopList;

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("STACKTRACE", e.toString());
        }
        return null;
    }
}
