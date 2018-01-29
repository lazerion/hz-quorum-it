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
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AcceptanceTest {

    private final Logger logger = LoggerFactory.getLogger(AcceptanceTest.class);
    private ComposeCli cli;

    @Before
    public void before() {
        cli = new ComposeCli();
    }

    @After
    public void after() {
        try {
            cli.down();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * This is a verification case after brains split and cluster size verified is equal to
     * quorum size
     * Current cluster size wait is at most 60 secs
     */
    @Test
    public void noQuorumExceptionWhenClientConnectedToSufficientQuorum() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 3;
        final int range = 3;

        cli.up("deployment-1.yaml").scale("hazelcast", initialClusterSize);
        ClientContainer client = new ClientContainer();

        IntStream.range(0, range).forEach(it -> {
            try {
                with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                        .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

                with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                        .untilAsserted(() -> assertTrue(client.run()));

                DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
                cli.networkDelay(".*hazelcast[_]{1}[4,5]{1}.*", resultHandler);

                with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                        .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

                resultHandler.waitFor();
                assertTrue(resultHandler.getExitValue() == 0);

                with().pollInterval(1, TimeUnit.SECONDS).await().atMost(5, SECONDS)
                        .untilAsserted(() -> assertNotNull(client.stop()));

                QuorumStatistics statistics = client.stop();

                logger.info("Statistics {}", statistics);
                assertTrue(statistics.getFailures() == 0);
                assertTrue(statistics.getExceptions() == 0);
                assertTrue(statistics.getQuorumExceptions() == 0);
                assertTrue(statistics.getSuccess() > 0);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * This is a verification case after brains split and cluster size verified under
     * quorum size
     * <p>
     * Current cluster size wait is at most 60 secs
     */
    @Test
    public void quorumExceptionWhenClientConnectedToUnderQuorumBrain() throws IOException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 2;
        final int range = 3;

        cli.up("deployment-1.yaml").scale("hazelcast", initialClusterSize);
        ClientContainer client = new ClientContainer();

        IntStream.range(0, range).forEach(it -> {
            try {
                with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                        .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

                DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
                cli.networkDelay(".*hazelcast[_]{1}[1,2,3]{1}.*", resultHandler);

                with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                        .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

                with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                        .untilAsserted(() -> assertTrue(client.run()));

                resultHandler.waitFor();
                assertTrue(resultHandler.getExitValue() == 0);

                with().pollInterval(1, TimeUnit.SECONDS).await().atMost(5, SECONDS)
                        .untilAsserted(() -> assertNotNull(client.stop()));

                QuorumStatistics statistics = client.stop();

                logger.info("Statistics {}", statistics);
                assertTrue(statistics.getFailures() == 0);
                assertTrue(statistics.getExceptions() != 0);
                assertTrue(statistics.getSuccess() == 0);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void noQuorumExceptionWhenOwnerGoesOffInSmallerBrain() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 3;

        cli.up("deployment-1.yaml").scale("hazelcast", initialClusterSize);
        ClientContainer client = new ClientContainer();

        with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                .untilAsserted(() -> assertTrue(client.run()));

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        cli.networkDelay(".*hazelcast[_]{1}[1,3]{1}.*", resultHandler);

        with().pollInterval(1, SECONDS).await().atMost(60, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

        resultHandler.waitFor();
        assertTrue(resultHandler.getExitValue() == 0);

        with().pollInterval(1, TimeUnit.SECONDS).await().atMost(5, SECONDS)
                .untilAsserted(() -> assertNotNull(client.stop()));

        QuorumStatistics statistics = client.stop();

        logger.info("Statistics {}", statistics);
        assertTrue(statistics.getFailures() == 0);
        assertTrue(statistics.getExceptions() != 0);
        assertTrue(statistics.getSuccess() == 0);
    }

    @Test
    public void noQuorumExceptionWhenMembersNetworkRemoved() throws IOException {
        final int initialClusterSize = 5;
        final int expectedHealthySize = 3;

        cli.up("deployment-2.yaml");
        ClientContainer client = new ClientContainer();

        with().pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                .untilAsserted(() -> assertTrue(client.run()));

        cli.networkDisconnect("shared", "hz-3");
        cli.networkDisconnect("shared", "hz-4");

        with().pollDelay(5, SECONDS).pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedHealthySize)));

        with().pollInterval(1, TimeUnit.SECONDS).await().atMost(5, SECONDS)
                .untilAsserted(() -> assertNotNull(client.stop()));

        QuorumStatistics statistics = client.stop();

        logger.info("Statistics {}", statistics);
        assertTrue(statistics.getFailures() == 0);
        assertTrue(statistics.getExceptions() == 0);
        assertTrue(statistics.getSuccess() > 0);
    }

    @Test
    public void quorumExceptionWhenMembersNetworkRemoved() throws IOException {
        final int initialClusterSize = 5;
        final int expectedUnhealthySize = 2;

        cli.up("deployment-3.yaml");
        ClientContainer client = new ClientContainer();

        with().pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        cli.networkDisconnect("shared", "hz-0");
        cli.networkDisconnect("shared", "hz-1");
        cli.networkDisconnect("shared", "hz-2");

        with().pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(expectedUnhealthySize)));

        with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                .untilAsserted(() -> assertTrue(client.run()));

        with().pollDelay(5, SECONDS).pollInterval(1, TimeUnit.SECONDS).await().atMost(10, SECONDS)
                .untilAsserted(() -> assertNotNull(client.stop()));

        QuorumStatistics statistics = client.stop();

        logger.info("Statistics {}", statistics);
        assertTrue(statistics.getFailures() == 0);
        assertTrue(statistics.getExceptions() != 0);
        assertTrue(statistics.getSuccess() == 0);
    }
}
