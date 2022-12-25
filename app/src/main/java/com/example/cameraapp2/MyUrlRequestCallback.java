package com.example.cameraapp2;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MyUrlRequestCallback extends UrlRequest.Callback {
    private static final String TAG = MyUrlRequestCallback.class.getSimpleName();

    private static final String NO_BUSES_RESPONSE_IDENTIFIER = "OGGI";

    public String responseBody;
    private final FragmentManager supportFragmentManager;
    private final String stopName;
    private final ProgressBar progressBar;
    private final TextView waitingForTperResponse;

    public MyUrlRequestCallback(FragmentManager fm, String stopName, ProgressBar progressBar, TextView waitingForTperResponse) {
        this.supportFragmentManager = fm;
        this.stopName = stopName;
        this.progressBar = progressBar;
        this.waitingForTperResponse = waitingForTperResponse;
        waitingForTperResponse.setVisibility(View.VISIBLE);
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
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws JSONException {
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

        //Properly format the response String
        responseBodyString = responseBodyString.trim().replaceAll("(\r\n|\n\r|\r|\n|\r0|\n0)", "");
        Log.d(TAG, "RESPONSEBODYSTRING: " + responseBodyString);
        if (responseBodyString.endsWith("0")) {
            responseBodyString = responseBodyString.substring(0, responseBodyString.length() - 1);
        }

        this.responseBody = responseBodyString;

        JSONObject results = new JSONObject();
        try {
            results.put("body", responseBodyString);
        } catch (JSONException e) {
            Log.e(TAG, "JSONEXCEPTION eccezione");
            e.printStackTrace();
        }

        progressBar.setVisibility(View.INVISIBLE);
        waitingForTperResponse.setVisibility(View.INVISIBLE);
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

    private List<List<String>> getMapFromJson(String jsonString) throws JSONException {
        List<List<String>> lista = new ArrayList<>();
        Log.i("JSONinviato", jsonString);
        JSONObject json = new JSONObject(jsonString);
        JSONObject message = json.getJSONObject("message");
        JSONArray buses = message.getJSONArray("Autobus");


        for (int i = 0; i < buses.length(); i++) {
            String line = buses.getJSONObject(i).getString("Line");
            if (line.startsWith(NO_BUSES_RESPONSE_IDENTIFIER)) {
                return new ArrayList<>();
            }
            String time = buses.getJSONObject(i).getString("Time");
            List<String> lineAndTime = new ArrayList<>();
            lineAndTime.add(line);
            lineAndTime.add(time);
            lista.add(lineAndTime);
        }

        return lista;
    }
}
