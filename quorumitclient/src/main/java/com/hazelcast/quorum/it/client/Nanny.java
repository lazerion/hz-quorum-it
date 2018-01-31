package com.hazelcast.quorum.it.client;


import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

class Nanny {
    private static Logger logger = LoggerFactory.getLogger(Nanny.class);

    private final static int DEFAULT_THREAD_COUNT = 10;
    private ExecutorService executorService;
    private QuorumStatistics statistics;
    private List<Future<Void>> futures;

    Nanny() {
        reset();
    }

    private void reset() {
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        this.statistics = new QuorumStatistics();
        this.futures = new LinkedList<>();
    }

    Boolean run(HazelcastInstance client) {
        if (!futures.isEmpty()) {
            logger.info("Already running data structures");
            statistics.reset();
            return true;
        }
        try {
            Collection<Callable<Void>> callables = new FixtureBuilder(client, statistics).callables();
            callables.forEach(it -> futures.add(executorService.submit(it)));
            return futures.size() > 0;
        } catch (Exception ex) {
            logger.error("exception on run-on {}", ex.getMessage(), ex);
            return false;
        }
    }

    Snapshot snapshot() {
        return statistics.snapshot();
    }

    Snapshot stop() {
        Snapshot snapshot = statistics.snapshot();
        try {
            futures.forEach(it -> it.cancel(true));
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("tasks interrupted {}", e.getMessage(), e);
        } finally {
            if (!executorService.isTerminated())
                executorService.shutdownNow();
            futures.clear();
        }
        return snapshot;
    }
}
