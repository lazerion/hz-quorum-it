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

    @Test
    public void NoQuorumExceptionWhenHealthyClusterSizeIsEqualQuorum() throws IOException, InterruptedException {
        final int initialClusterSize = 5;
        final int expectedClusterSize = 3;

        cli.up("deployment-1.yaml").scale("hazelcast", initialClusterSize);
        ClientContainer client = new ClientContainer();

        with().pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                .untilAsserted(() -> assertTrue(client.sanity(initialClusterSize)));

        IntStream.range(0, 3).forEach(it -> {
            try {
                with().pollInterval(1, SECONDS).await().atMost(5, SECONDS)
                        .untilAsserted(() -> assertTrue(client.run()));

                DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
                cli.networkDelay(".*hazelcast[_]{1}[4,5]{1}.*", resultHandler);

                with().pollInterval(1, SECONDS).await().atMost(20, SECONDS)
                        .untilAsserted(() -> assertTrue(client.sanity(expectedClusterSize)));

                resultHandler.waitFor();
                assertTrue(resultHandler.getExitValue() == 0);

                with().pollInterval(1, TimeUnit.SECONDS).await().atMost(20, SECONDS)
                        .untilAsserted(() -> assertNotNull(client.stop()));

                QuorumStatistics statistics = client.stop();

                assertTrue(statistics.getFailures() == 0);
                assertTrue(statistics.getExceptions() == 0);
                assertTrue(statistics.getSuccess() > 0);
            }catch (Exception e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
