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
    private static Logger logger = LoggerFactory.getLogger(FixtureBuilder.class);

    private static int FAIL_SAFE_TIMEOUT = 100;

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
                cardinalityEstimator(),
                countDownLatch(),
                multiMap()
        )
                .map(it -> it.statistics(statistics))
                .map(it -> it.timeout(FAIL_SAFE_TIMEOUT)) // timeout effects fails
                .map(QuorumTask.QuorumTaskBuilder::build)
                .peek(it -> logger.info("Created callable {}", it.toString()))
                .collect(Collectors.toList());
    }

    private QuorumTask.QuorumTaskBuilder list() {
        final IList<String> list = client.getList("default");
        return QuorumTask.builder()
                .write(list::add)
                .read(list::contains)
                .test(it -> !list.isEmpty())
                .name(IList.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder set() {
        final ISet<String> set = client.getSet("default");
        return QuorumTask.builder()
                .write(set::add)
                .read(set::contains)
                .test(it -> !set.isEmpty())
                .name(ISet.class.getName());
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
                .test(it -> semaphore.availablePermits() == permits)
                .name(ISemaphore.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder ringBuffer() {
        final Ringbuffer<String> ringBuffer = client.getRingbuffer("default");

        return QuorumTask.builder()
                .write(it -> ringBuffer.add(it) != -1)
                .read(it -> ringBuffer.tailSequence() != -1)
                .test(it -> ringBuffer.size() > 0)
                .name(Ringbuffer.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder replicatedMap() {
        final ReplicatedMap<String, String> replicatedMap = client.getReplicatedMap("default");
        return QuorumTask.builder()
                .write(it -> replicatedMap.put(it, RandomStringUtils.randomAlphabetic(42)) == null)
                .read(it -> replicatedMap.get(it) != null)
                .test(it -> !replicatedMap.isEmpty())
                .name(ReplicatedMap.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder atomicLong() {
        final IAtomicLong atomicLong = client.getAtomicLong("default");
        atomicLong.set(0);
        return QuorumTask.builder()
                .write(it -> atomicLong.incrementAndGet() > 0)
                .read(it -> atomicLong.get() > 0)
                .test(it -> atomicLong.decrementAndGet() == 0)
                .name(IAtomicLong.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder atomicReference() {
        final IAtomicReference<String> atomicReference = client.getAtomicReference("default");
        return QuorumTask.builder()
                .write(it -> {
                    try {
                        atomicReference.set(it);
                        return true;
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                        return false;
                    }
                })
                .test(it -> !atomicReference.isNull())
                .read(it -> StringUtils.isNotBlank(atomicReference.get()))
                .name(IAtomicReference.class.getName());
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
                .test(it -> true)
                .name(CardinalityEstimator.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder countDownLatch() {
        final ICountDownLatch countDownLatch = client.getCountDownLatch("default");
        final int count = 1;
        return QuorumTask.builder()
                .write(it -> countDownLatch.trySetCount(count))
                .read(it -> countDownLatch.getCount() == count)
                .test(it -> {
                    countDownLatch.countDown();
                    return true;
                })
                .name(ICountDownLatch.class.getName());
    }

    private QuorumTask.QuorumTaskBuilder multiMap() {
        final MultiMap<String, String> multiMap = client.getMultiMap("default");
        return QuorumTask.builder()
                .write(it -> multiMap.put(it, RandomStringUtils.randomAlphabetic(42)))
                .read(it -> !multiMap.get(it).isEmpty())
                .test(it -> multiMap.size() != 0)
                .name(MultiMap.class.getName());
    }
}
