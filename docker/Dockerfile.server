ARG BASE_SERVER_IMAGE=starwhaleai/base_server:latest
FROM ${BASE_SERVER_IMAGE}

# java opts
ENV JDWP_PORT=5005
ENV JVM_XMX=2048M
ENV JVM_XMS=512M
ENV JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:$JDWP_PORT
ENV JAR=agent

WORKDIR /opt/starwhale.java
# java jar
COPY jar/agent.jar agent.jar
COPY jar/controller.jar controller.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Xms$JVM_XMS -Xmx$JVM_XMX -jar $JAR.jar"]