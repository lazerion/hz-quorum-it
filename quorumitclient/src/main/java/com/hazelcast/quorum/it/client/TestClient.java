package com.hazelcast.quorum.it.client;


import com.google.gson.Gson;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static spark.Spark.get;

public class TestClient {
    private static Logger logger = LoggerFactory.getLogger(TestClient.class);
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

    private static QuorumStatistics stop() {
        return nanny.stop();
    }

    private static String run() {
        return nanny.runOn(client);
    }

    private static Boolean sanity(String count) {
        with().pollInterval(1, TimeUnit.SECONDS).await().atMost(20, SECONDS)
                .until(() -> client.getCluster().getMembers().size() == Integer.parseInt(count));
        return true;
    }

    private static void startClient() throws InterruptedException {
        ClientConfig clientConfig = new XmlClientConfigBuilder().build();

        logger.info("Group {}", clientConfig.getGroupConfig().getName());
        logger.info("Password {}", clientConfig.getGroupConfig().getPassword());

        client = HazelcastClient.newHazelcastClient(clientConfig);
    }
}
