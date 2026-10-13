FROM eclipse-temurin:21-jdk-alpine AS build
ARG MAVEN_PROFILE=prod
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -q -P${MAVEN_PROFILE} dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests -P${MAVEN_PROFILE} package \
    && grep -q "active: \"${MAVEN_PROFILE}\"" target/classes/application.yaml \
    && cp target/tusalud-api-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /workspace/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

FROM eclipse-temurin:21-jre-alpine AS runtime
ARG OTEL_JAVA_AGENT_VERSION=2.32.0
ARG MAVEN_PROFILE=prod
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

ADD --chown=spring:spring \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar \
    /otel/opentelemetry-javaagent.jar
COPY --chown=spring:spring docker/otel-agent/agent.properties /otel/agent.properties

COPY --from=extract --chown=spring:spring /extract/layers/dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=spring:spring /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/layers/application/ ./

USER spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=${MAVEN_PROFILE} \
    TZ=America/Lima \
    OTEL_JAVAAGENT_CONFIGURATION_FILE=/otel/agent.properties \
    OTEL_SERVICE_NAME=tusalud-api \
    OTEL_RESOURCE_ATTRIBUTES=service.namespace=tusalud,deployment.environment.name=${MAVEN_PROFILE} \
    OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318 \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=otlp \
    OTEL_LOGS_EXPORTER=otlp

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -q -O /dev/null "http://localhost:${SERVER_PORT}/v3/api-docs" || exit 1

ENTRYPOINT ["sh", "-c", "exec java -javaagent:/otel/opentelemetry-javaagent.jar $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
