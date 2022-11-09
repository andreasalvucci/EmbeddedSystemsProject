package com.example.cameraapp2;

import android.content.SharedPreferences;
import android.util.Log;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class MyUrlRequestCallback extends UrlRequest.Callback{
    private static final String TAG = MyUrlRequestCallback.class.getSimpleName();

    public String responseBody;




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
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws UnsupportedEncodingException, JSONException {
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
        if (responseBodyString.endsWith("0")) {
            responseBodyString = responseBodyString.substring(0, responseBodyString.length()-1);
        }

        this.responseBody = responseBodyString;

        Map<String, List<String>> headers = info.getAllHeaders(); //get headers

        String reqHeaders = createHeaders(headers);

        JSONObject results = new JSONObject();
        try {
            results.put("headers", reqHeaders);
            results.put("body", responseBodyString);
        } catch (JSONException e ) {
            e.printStackTrace();
        }
        Log.d(TAG, results.get("body").toString());
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

            if (accTok.size() > 0) {
                accessToken = accTok.get(accTok.size()-1);
            }
        }

        if (headers.containsKey("Client")) {
            List<String> cl = headers.get("Client");

            if (cl.size() > 0) {
                client = cl.get(cl.size()-1);
            }
        }

        if (headers.containsKey("Uid")) {
            List<String> u = headers.get("Uid");

            if (u.size() > 0) {
                uid = u.get(u.size()-1);
            }
        }

        if (headers.containsKey("Expiry")) {
            List<String> ex = headers.get("Expiry");

            if (ex.size() > 0) {
                expiry = Long.parseLong(ex.get(ex.size()-1));
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
        public void onFinishRequest(JSONObject result);

    }

}
