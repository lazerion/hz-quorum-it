package com.hazelcast.quorum.it.client;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
class Snapshot {
    @SerializedName("success")
    private int success;
    @SerializedName("failures")
    private int failures;
    @SerializedName("exceptions")
    private int exceptions;
    @SerializedName("quorumExceptions")
    private int quorumExceptions;
}
