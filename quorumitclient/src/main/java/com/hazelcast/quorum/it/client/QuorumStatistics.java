package com.hazelcast.quorum.it.client;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
class QuorumStatistics {
    private AtomicInteger success = new AtomicInteger(0);
    private AtomicInteger failures = new AtomicInteger(0);
    private AtomicInteger exceptions = new AtomicInteger(0);
    private AtomicInteger quorumExceptions = new AtomicInteger(0);

    Snapshot snapshot() {
        return Snapshot.builder()
                .exceptions(this.exceptions.intValue())
                .success(this.success.intValue())
                .failures(this.failures.intValue())
                .quorumExceptions(this.quorumExceptions.intValue())
                .build();
    }
}
