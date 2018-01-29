package com.hazelcast.quorum.it.client;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
class QuorumStatistics {
    @SerializedName("success")
    private AtomicInteger success = new AtomicInteger(0);
    @SerializedName("failures")
    private AtomicInteger failures = new AtomicInteger(0);
    @SerializedName("exceptions")
    private AtomicInteger exceptions = new AtomicInteger(0);
    @SerializedName("quorumExceptions")
    private AtomicInteger quorumExceptions = new AtomicInteger(0);
}
