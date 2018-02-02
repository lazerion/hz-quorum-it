package com.hazelcast.quorum.it.client;

import java.io.Serializable;

public class EchoTask implements Runnable, Serializable {

    private final String msg;

    EchoTask(String msg) {
        this.msg = msg;
    }

    @Override
    public void run() {
        System.out.println("Echo: " + msg);
    }
}
