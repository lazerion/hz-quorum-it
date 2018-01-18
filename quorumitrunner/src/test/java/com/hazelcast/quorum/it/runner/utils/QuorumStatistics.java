package com.hazelcast.quorum.it.runner.utils;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class QuorumStatistics {
    @SerializedName("success")
    private Integer success;
    @SerializedName("failures")
    private Integer failures;
    @SerializedName("exceptions")
    private Integer exceptions;
}
