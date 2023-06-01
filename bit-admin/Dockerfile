FROM findepi/graalvm:java17-native

MAINTAINER Lion Li

RUN mkdir -p /bit/server/logs \
    /bit/server/temp \
    /bit/skywalking/agent

WORKDIR /bit/server

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ADD ./target/bit-admin.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
            # 应用名称 如果想区分集群节点监控 改成不同的名称即可
#            "-Dskywalking.agent.service_name=bit-server", \
#            "-javaagent:/bit/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]
