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
    private final ExecutorService executorService;
    private final QuorumStatistics statistics;
    private final List<Future> futures;

    Nanny() {
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        this.statistics = new QuorumStatistics();
        this.futures = new LinkedList<>();
    }

    Boolean runOn(HazelcastInstance client) {
        try {
            Collection<Callable<Void>> callables = new FixtureBuilder(client, statistics).callables();
            callables.forEach(it -> futures.add(executorService.submit(it)));
            return futures.size() > 0;
        } catch (Exception ex) {
            logger.error("exception on runOn {}", ex.getMessage());
            return false;
        }
    }

    QuorumStatistics stop() {
        futures.forEach(it -> it.cancel(true));
        return statistics;
    }
}
