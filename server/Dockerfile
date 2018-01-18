FROM openjdk:8-jre
ENV HZ_VERSION 3.10-SNAPSHOT
ENV HZ_HOME /opt/hazelcast/
RUN mkdir -p $HZ_HOME
WORKDIR $HZ_HOME
# Download hazelcast jars from maven repo.
ADD hazelcast-all-3.10-SNAPSHOT.jar $HZ_HOME

ADD server.sh /$HZ_HOME/server.sh
RUN chmod +x /$HZ_HOME/server.sh
EXPOSE 5701
# Start hazelcast standalone server.
CMD ["./server.sh"]
