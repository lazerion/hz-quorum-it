package com.hazelcast.quorum.it.runner.utils;


import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientContainer {
    private final Logger logger = LoggerFactory.getLogger(ClientContainer.class);
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private OkHttpClient client = new OkHttpClient();

    public ClientContainer() {
        baseUrl = "http://localhost:4567";
    }


    public boolean sanity(int clusterSize) {
        Request request = new Request.Builder()
                .url(String.format("%s/sanity/%d", baseUrl, clusterSize))
                .build();

        return sendRequest(request);
    }

    private boolean sendRequest(Request request) {
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.error("error {}", response.message());
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean run() {
        Request request = new Request.Builder()
                .url(String.format("%s/run", baseUrl))
                .build();

        return sendRequest(request);
    }

    public QuorumStatistics stop() {
        Request request = new Request.Builder()
                .url(String.format("%s/stop", baseUrl))
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                return null;
            }
            final String snap = response.body().string();
            return new Gson().fromJson(snap, QuorumStatistics.class);
        } catch (Exception ex) {
            return null;
        }

    }
}
