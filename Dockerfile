ARG JDK_IMAGE=eclipse-temurin:25-jdk-alpine-3.24
ARG RUNTIME_IMAGE=alpine:3.24

FROM ${JDK_IMAGE} AS build
ARG MAVEN_PROFILE=prod
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -P${MAVEN_PROFILE} dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests -P${MAVEN_PROFILE} package \
    && grep -q "active: \"${MAVEN_PROFILE}\"" target/classes/application.yaml \
    && cp target/tusalud-api-0.0.1-SNAPSHOT.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination /layers

FROM ${JDK_IMAGE} AS jre
COPY --from=build /layers /layers
RUN jdeps --ignore-missing-deps -q --recursive --multi-release 25 --print-module-deps \
        --class-path "$(find /layers -path '*/lib/*.jar' | tr '\n' ':')" \
        "$(find /layers -maxdepth 2 -name '*.jar' | head -1)" > /tmp/modules \
    && jlink \
        --add-modules "$(cat /tmp/modules),java.instrument,java.management,java.net.http,java.security.jgss,java.security.sasl,jdk.jfr,jdk.localedata,jdk.management,jdk.naming.dns,jdk.unsupported,jdk.zipfs" \
        --include-locales=en,es \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-9 \
        --generate-cds-archive \
        --output /jre

FROM ${RUNTIME_IMAGE} AS base
ARG OTEL_JAVA_AGENT_VERSION=2.32.0
ARG OTEL_JAVA_AGENT_SHA256=f787eb6c7f3d18e69a431e108a15278d25ee37f83d68b678f621e063f3988f82
ENV JAVA_HOME=/opt/java \
    PATH=/opt/java/bin:$PATH \
    TZ=America/Lima \
    LANG=C.UTF-8
RUN addgroup -S -g 10001 spring && adduser -S -u 10001 -G spring -h /app spring && mkdir -m 755 /otel
WORKDIR /app
COPY --from=jre /jre /opt/java
ADD --checksum=sha256:${OTEL_JAVA_AGENT_SHA256} --chmod=644 \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar \
    /otel/opentelemetry-javaagent.jar
COPY docker/otel-agent/agent.properties /otel/agent.properties
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

FROM base AS runtime
ARG MAVEN_PROFILE=prod
LABEL org.opencontainers.image.title="tusalud-api" \
      org.opencontainers.image.description="API REST de TuSalud" \
      org.opencontainers.image.source="https://github.com/jhonsferg/tusalud" \
      org.opencontainers.image.vendor="CodeSoftLabs"
USER spring:spring
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=${MAVEN_PROFILE} \
    JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    OTEL_JAVAAGENT_CONFIGURATION_FILE=/otel/agent.properties \
    OTEL_SERVICE_NAME=tusalud-api \
    OTEL_RESOURCE_ATTRIBUTES=service.namespace=tusalud,deployment.environment.name=${MAVEN_PROFILE} \
    OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318 \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=otlp \
    OTEL_LOGS_EXPORTER=otlp
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" || exit 1
ENTRYPOINT ["java", "-javaagent:/otel/opentelemetry-javaagent.jar", "-XX:+UseCompactObjectHeaders", "-jar", "/app/app.jar"]
