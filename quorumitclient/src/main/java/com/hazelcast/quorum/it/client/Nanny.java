package com.hazelcast.quorum.it.client;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

class Nanny {
    private static Logger logger = LoggerFactory.getLogger(Nanny.class);

    private final static int DEFAULT_THREAD_COUNT = 1;
    private final ExecutorService executorService;
    private final QuorumStatistics statistics;
    private final List<Future> futures;

    Nanny() {
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        this.statistics = new QuorumStatistics();
        this.futures = new LinkedList<>();
    }

    Boolean runOn(HazelcastInstance client) {
        final IList<String> list = client.getList("default");
        Callable<Void> task = () -> {
            while (true) {
                try {
                    if (!list.add(RandomStringUtils.randomAlphanumeric(42))) {
                        statistics.getFailures().incrementAndGet();
                    } else {
                        statistics.getSuccess().incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.error("Exception during operation {}", e.getMessage(), e);
                    statistics.getExceptions().incrementAndGet();
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    return null;
                }
            }
        };
        return futures.add(executorService.submit(task));
    }

    QuorumStatistics stop() {
        futures.forEach(it -> {
            it.cancel(true);
        });
        return statistics;
    }
}
