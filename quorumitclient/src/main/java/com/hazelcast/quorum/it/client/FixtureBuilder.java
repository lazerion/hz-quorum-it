package com.hazelcast.quorum.it.client;


import com.hazelcast.cardinality.CardinalityEstimator;
import com.hazelcast.core.*;
import com.hazelcast.ringbuffer.Ringbuffer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class FixtureBuilder {
    private static Logger logger = LoggerFactory.getLogger(Nanny.class);

    private final HazelcastInstance client;
    private final QuorumStatistics statistics;

    FixtureBuilder(HazelcastInstance client, QuorumStatistics statistics) {
        this.client = client;
        this.statistics = statistics;
    }

    Collection<Callable<Void>> callables() {
        return Stream.of(
                list(),
                set(),
                semaphore(),
                ringBuffer(),
                replicatedMap(),
                atomicLong(),
                atomicReference(),
                cardinalityEstimator()
        )
                .map(it -> it.statistics(statistics))
                .map(it -> it.timeout(30))
                .map(QuorumTask.QuorumTaskBuilder::build)
                .collect(Collectors.toList());
    }

    private QuorumTask.QuorumTaskBuilder list() {
        final IList<String> list = client.getList("default");
        return QuorumTask.builder()
                .write(list::add)
                .read(list::contains)
                .test(it -> !list.isEmpty());
    }

    private QuorumTask.QuorumTaskBuilder set() {
        final ISet<String> set = client.getSet("default");
        return QuorumTask.builder()
                .write(set::add)
                .read(set::contains)
                .test(it -> !set.isEmpty());
    }

    private QuorumTask.QuorumTaskBuilder semaphore() {
        final ISemaphore semaphore = client.getSemaphore("default");
        final int permits = 5;
        if (!semaphore.init(permits)) {
            logger.warn("Semaphore default available permits {}", semaphore.availablePermits());
        }

        return QuorumTask.builder()
                .write(it -> semaphore.tryAcquire())
                .read(it -> {
                    try {
                        semaphore.release();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .test(it -> semaphore.availablePermits() == permits);
    }

    private QuorumTask.QuorumTaskBuilder ringBuffer() {
        final Ringbuffer<String> ringBuffer = client.getRingbuffer("default");
        return QuorumTask.builder()
                .write(it -> ringBuffer.add(it) != -1)
                .read(it -> ringBuffer.tailSequence() != -1)
                .test(it -> ringBuffer.size() > 0);
    }

    private QuorumTask.QuorumTaskBuilder replicatedMap() {
        final ReplicatedMap<String, String> replicatedMap = client.getReplicatedMap("default");
        return QuorumTask.builder()
                .write(it -> replicatedMap.put(it, RandomStringUtils.randomAlphabetic(42)) == null)
                .read(it -> replicatedMap.get(it) != null)
                .test(it -> !replicatedMap.isEmpty());
    }

    private QuorumTask.QuorumTaskBuilder atomicLong() {
        final IAtomicLong atomicLong = client.getAtomicLong("default");
        return QuorumTask.builder()
                .write(it -> atomicLong.incrementAndGet() > 0)
                .read(it -> atomicLong.get() > 0)
                .test(it -> atomicLong.decrementAndGet() == 0);
    }

    private QuorumTask.QuorumTaskBuilder atomicReference() {
        final IAtomicReference<String> atomicReference = client.getAtomicReference("default");
        return QuorumTask.builder()
                .write(it -> {
                    try {
                        atomicReference.set(it);
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .test(it -> !atomicReference.isNull())
                .read(it -> StringUtils.isNotBlank(atomicReference.get()));
    }

    private QuorumTask.QuorumTaskBuilder cardinalityEstimator() {
        final CardinalityEstimator cardinalityEstimator = client.getCardinalityEstimator("default");
        return QuorumTask.builder()
                .write(it -> {
                    try {
                        cardinalityEstimator.add(it);
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .read(it -> {
                    long estimate = cardinalityEstimator.estimate();
                    return estimate != 0L;
                })
                .test(it -> true);
    }
}
