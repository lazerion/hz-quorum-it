package com.hazelcast.quorum.it.runner.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class ComposeCli {
    private File project;

    public ComposeCli up(String deployment) throws IOException {
        if (StringUtils.isBlank(deployment)) {
            throw new IllegalArgumentException("Deployment can not be blank");
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(deployment);
        File file = new File(url.getPath());

        if (!file.exists()) {
            throw new IllegalArgumentException("Deployment file does not exist");
        }

        this.project = file;

        String line = String.format("docker-compose -f %s up -d", this.project.getAbsoluteFile());

        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        int status = executor.execute(cmdLine);
        assertTrue(status == 0);

        return this;
    }

    public ComposeCli down() throws IOException {
        String line = String.format("docker-compose -f %s down", this.project.getAbsoluteFile());
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        int status = executor.execute(cmdLine);
        assertTrue(status == 0);
        return this;
    }

    public ComposeCli scale(String service, int count) throws IOException {
        String line = String.format("docker-compose -f %s scale %s=%d", this.project.getAbsoluteFile(), service, count);
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        int status = executor.execute(cmdLine);
        assertTrue(status == 0);
        return this;
    }

    public ComposeCli networkDelay(String re2, DefaultExecuteResultHandler resultHandler) throws IOException {
        //requires pumba
        String line = String.format("pumba netem --tc-image gaiadocker/iproute2 --duration 30s delay --time 5000 re2:%s", re2);
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        executor.execute(cmdLine, resultHandler);
        return this;
    }

    public ComposeCli networkDisconnect(String network, String container) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String line = String.format("docker network ls --format {{.Name}} --filter name=%s", network);
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        exec.execute(CommandLine.parse(line));

        String networkName = outputStream.toString();
        line = String.format("docker network disconnect %s %s", networkName, container);
        int status = exec.execute(CommandLine.parse(line));
        assertTrue(status == 0);
        return this;
    }
}
