package com.hazelcast.quorum.it.runner;


import com.hazelcast.quorum.it.runner.utils.ClientContainer;
import com.hazelcast.quorum.it.runner.utils.ComposeCli;
import com.hazelcast.quorum.it.runner.utils.QuorumStatistics;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AcceptanceTest {

    private final Logger logger = LoggerFactory.getLogger(AcceptanceTest.class);
    private ComposeCli cli;
    private ClientContainer client;

    private static int CLUSTER_WAIT = 30;
    private static int OPERATION_WAIT = 10;

    @Before
    public void before() {
        client = new ClientContainer();
        cli = new ComposeCli();
    }

    @After
    public void after() {
        try {
            cli.down();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        try {
            client.stop();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * This is a verification case after brains split and cluster size verified is equal to
     * quorum size
     * Current cluster size wait is at most {@code CLUSTER_WAIT} secs
     */
    @Test
    public void noQuorumExceptionWhenClientConnectedToMajority() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 3;

        cli.up("deployment-1.yaml");
        with().pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(1)));

        cli.scale("hazelcast", initialClusterSize);

        with().pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        assertTrue(client.run());

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        cli.networkDelay(".*hazelcast[_]{1}[4,5]{1}.*", resultHandler);

        with().pollDelay(OPERATION_WAIT, SECONDS)
                .pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

        resultHandler.waitFor();
        assertTrue(resultHandler.getExitValue() == 0);

        QuorumStatistics statistics = client.snapshot();
        assertNotNull(statistics);

        logger.info("Statistics {}", statistics);
        if (statistics.getFailures() != 0 ||
                statistics.getExceptions() != 0 ||
                statistics.getQuorumExceptions() != 0) {
            cli.logs();
        }
        assertTrue(statistics.getQuorumExceptions() == 0);
        assertTrue(statistics.getSuccess() != 0);
    }

    /**
     * This is a verification case after brains split and cluster size verified under
     * quorum size
     * <p>
     * Current cluster size wait is at {@code CLUSTER_WAIT} secs
     */
    @Test
    public void quorumExceptionWhenClientConnectedToMinority() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 2;

        cli.up("deployment-4.yaml");
        with().pollInterval(1, SECONDS).await().atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(1)));

        cli.scale("hazelcast", initialClusterSize);
        with().pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        assertTrue(client.run());

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        cli.networkDelay(".*hazelcast[_]{1}[1,2,3]{1}.*", resultHandler);

        with().pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

        //reset statistics
        assertTrue(client.run());
        //let it run with fresh statistics
        TimeUnit.SECONDS.sleep(OPERATION_WAIT);

        QuorumStatistics statistics = client.snapshot();
        assertNotNull(statistics);

        resultHandler.waitFor();
        assertTrue(resultHandler.getExitValue() == 0);

        logger.info("Statistics {}", statistics);
        assertTrue(statistics.getQuorumExceptions() != 0);
        assertTrue(statistics.getSuccess() == 0);
    }

    @Test
    public void noQuorumExceptionWhenOwnerGoesOffInMinority() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 3;

        cli.up("deployment-1.yaml");
        with().pollInterval(1, SECONDS).await().atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(1)));

        cli.scale("hazelcast", initialClusterSize);

        with().pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        assertTrue(client.run());

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        cli.networkDelay(".*hazelcast[_]{1}[1,3]{1}.*", resultHandler);

        with().pollDelay(OPERATION_WAIT, SECONDS)
                .pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

        resultHandler.waitFor();
        assertTrue(resultHandler.getExitValue() == 0);

        QuorumStatistics statistics = client.snapshot();
        assertNotNull(statistics);

        logger.info("Statistics {}", statistics);
        if (statistics.getFailures() != 0 ||
                statistics.getExceptions() != 0 ||
                statistics.getQuorumExceptions() != 0) {
            cli.logs();
        }
        assertTrue(statistics.getQuorumExceptions() == 0);
        assertTrue(statistics.getSuccess() != 0);
    }

    /**
     * redo operation is enabled
     */
    @Test
    public void noQuorumExceptionWhenMembersNetworkRemoved() throws IOException {
        final int initialClusterSize = 5;
        final int expectedHealthySize = 3;

        cli.up("deployment-2.yaml");
        with().pollInterval(1, SECONDS).await().atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        assertTrue(client.run());
        cli.networkDisconnect("shared", "hz-3");
        cli.networkDisconnect("shared", "hz-4");

        with().pollDelay(OPERATION_WAIT, SECONDS)
                .pollInterval(1, SECONDS)
                .await()
                .atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedHealthySize)));

        QuorumStatistics statistics = client.snapshot();
        assertNotNull(statistics);

        logger.info("Statistics {}", statistics);
        if (statistics.getFailures() != 0) {
            cli.logs();
        }

        assertTrue(statistics.getQuorumExceptions() == 0);
        assertTrue(statistics.getSuccess() != 0);
    }

    @Test
    public void quorumExceptionWhenMembersNetworkRemoved() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedUnhealthySize = 2;

        cli.up("deployment-3.yaml");
        with().pollInterval(1, SECONDS).await().atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        assertTrue(client.run());
        cli.networkDisconnect("shared", "hz-0");
        cli.networkDisconnect("shared", "hz-1");
        cli.networkDisconnect("shared", "hz-2");

        with().pollInterval(1, SECONDS).await().atMost(CLUSTER_WAIT, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedUnhealthySize)));

        //reset statistics
        assertTrue(client.run());
        //let it run with fresh statistics
        TimeUnit.SECONDS.sleep(OPERATION_WAIT);

        QuorumStatistics statistics = client.snapshot();
        assertNotNull(statistics);

        logger.info("Statistics {}", statistics);
        assertTrue(statistics.getQuorumExceptions() != 0);
        assertTrue(statistics.getSuccess() == 0);
    }
}
