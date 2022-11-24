package com.example.cameraapp2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentManager;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.JSONParser.*;
import org.json.simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyUrlRequestCallback extends UrlRequest.Callback {
    private static final String TAG = MyUrlRequestCallback.class.getSimpleName();

    public String responseBody;
    private Context context;
    private FragmentManager supportFragmentManager;
    private String stopName;
    private ProgressBar progressBar;

    public MyUrlRequestCallback(FragmentManager fm, String stopName, ProgressBar progressBar) {
        this.supportFragmentManager = fm;
        this.stopName = stopName;
        this.progressBar = progressBar;
    }


    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        Log.i(TAG, "onRedirectReceived method called.");
        // You should call the request.followRedirect() method to continue
        // processing the request.
        request.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        Log.i(TAG, "onResponseStarted method called.");
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes for the read() method. The same buffer
        // with data is passed to the onReadCompleted() method.
        request.read(ByteBuffer.allocateDirect(102400));
    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws UnsupportedEncodingException, JSONException, ParseException {
        Log.i(TAG, "onReadCompleted method called.");
        // You should keep reading the request until there's no more data.

        byteBuffer.clear();
        request.read(byteBuffer);
        byte[] bytes;
        if (byteBuffer.hasArray()) {
            bytes = byteBuffer.array();
        } else {
            bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
        }

        String responseBodyString = new String(bytes);

        //Convert bytes to string

        //Properly format the response String
        responseBodyString = responseBodyString.trim().replaceAll("(\r\n|\n\r|\r|\n|\r0|\n0)", "");
        Log.d(TAG, "RESPONSE_BODY_STRING: " + responseBodyString);
        if (responseBodyString.endsWith("0")) {
            responseBodyString = responseBodyString.substring(0, responseBodyString.length() - 1);
        }

        this.responseBody = responseBodyString;

        Map<String, List<String>> headers = info.getAllHeaders(); //get headers

        String reqHeaders = createHeaders(headers);

        JSONObject results = new JSONObject();
        try {
            // results.put("headers", reqHeaders);
            results.put("body", responseBodyString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        progressBar.setVisibility(View.INVISIBLE);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getMapFromJson(responseBodyString), this.stopName);
        bottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet");

    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        Log.i(TAG, "onSucceeded method called.");
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        Log.e(TAG, "The request failed.", error);
    }

    private String createHeaders(Map<String, List<String>> headers) {

        String accessToken = "null";
        String client = "null";
        String uid = "null";
        long expiry = 0;

        if (headers.containsKey("Access-Token")) {
            List<String> accTok = headers.get("Access-Token");

            if (accTok != null && !accTok.isEmpty()) {
                accessToken = accTok.get(accTok.size() - 1);
            }
        }

        if (headers.containsKey("Client")) {
            List<String> cl = headers.get("Client");

            if (cl != null && !cl.isEmpty()) {
                client = cl.get(cl.size() - 1);
            }
        }

        if (headers.containsKey("Uid")) {
            List<String> u = headers.get("Uid");

            if (u != null && !u.isEmpty()) {
                uid = u.get(u.size() - 1);
            }
        }

        if (headers.containsKey("Expiry")) {
            List<String> ex = headers.get("Expiry");

            if (ex != null && ex.size() > 0) {
                expiry = Long.parseLong(ex.get(ex.size() - 1));
            }
        }

        JSONObject currentHeaders = new JSONObject();
        try {
            currentHeaders.put("access-token", accessToken);
            currentHeaders.put("client", client);
            currentHeaders.put("uid", uid);
            currentHeaders.put("expiry", expiry);

            return currentHeaders.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return currentHeaders.toString();
    }

    public interface OnFinishRequest<JSONObject> {
        void onFinishRequest(JSONObject result);
    }

    private List<List<String>> getMapFromJson(String jsonString) throws JSONException, ParseException {
        List<List<String>> lista = new ArrayList<>();
        Log.i("JSONinviato", jsonString);
        JSONObject json = new JSONObject(jsonString);
        JSONObject message = json.getJSONObject("message");
        JSONArray buses = message.getJSONArray("Autobus");

        for (int i = 0; i < buses.length(); i++) {
            String line = buses.getJSONObject(i).getString("Line");
            String time = buses.getJSONObject(i).getString("Time");
            List<String> lineAndTime = new ArrayList<>();
            lineAndTime.add(line);
            lineAndTime.add(time);
            lista.add(lineAndTime);

        }

        return lista;
    }
}
