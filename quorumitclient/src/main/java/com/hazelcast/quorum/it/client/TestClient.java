package com.hazelcast.quorum.it.client;


import com.google.gson.Gson;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import static spark.Spark.get;

public class TestClient {
    private static HazelcastInstance client;
    private static Nanny nanny = new Nanny();

    public static void main(String[] args) throws InterruptedException {
        startServer();
        startClient();
    }

    private static void startServer() {
        get("/sanity/:count", (req, res) -> sanity(req.params(":count")));
        get("/run", (req, res) -> run());
        get("/stop", (req, res) -> new Gson().toJson(stop()));
    }

    private static Snapshot stop() {
        return nanny.stop();
    }

    private static Boolean run() {
        return nanny.runOn(client);
    }

    private static Boolean sanity(String count) {
        return client.getCluster().getMembers().size() == Integer.parseInt(count);
    }

    private static void startClient() throws InterruptedException {
        ClientConfig clientConfig = new XmlClientConfigBuilder().build();
        client = HazelcastClient.newHazelcastClient(clientConfig);
    }
}
