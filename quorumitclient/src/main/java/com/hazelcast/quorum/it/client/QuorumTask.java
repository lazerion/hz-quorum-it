package com.hazelcast.quorum.it.client;


import com.hazelcast.quorum.QuorumException;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
@Builder
@ToString
public class QuorumTask implements Callable<Void> {
    private static Logger logger = LoggerFactory.getLogger(QuorumTask.class);

    private Function<String, Boolean> write;
    private Function<String, Boolean> read;
    private Predicate<Void> test;
    private QuorumStatistics statistics;
    private String name;
    private long timeout = 10;

    @Override
    public Void call() throws Exception {
        while (true) {
            try {
                String key = RandomStringUtils.randomAlphanumeric(42);
                if (write.apply(key)) {
                    statistics.getSuccess().incrementAndGet();
                } else {
                    logger.warn("{} write fails", name);
                    statistics.getFailures().incrementAndGet();
                }

                if (read.apply(key)) {
                    statistics.getSuccess().incrementAndGet();
                } else {
                    logger.warn("{} read fails", name);
                    statistics.getFailures().incrementAndGet();
                }

                if (test.test(null)) {
                    statistics.getSuccess().incrementAndGet();
                } else {
                    logger.warn("{} test fails", name);
                    statistics.getFailures().incrementAndGet();
                }

            } catch (QuorumException e) {
                logger.error("Quorum exception during operation {}", e.getMessage(), e);
                statistics.getQuorumExceptions().incrementAndGet();
            } catch (Exception e) {
                if (StringUtils.containsIgnoreCase(e.getMessage(), "InterruptedException")) {
                    return null;
                }
                logger.error("Exception during operation {}", e.getMessage(), e);
                statistics.getExceptions().incrementAndGet();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(timeout);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                return null;
            }
        }
    }
}
