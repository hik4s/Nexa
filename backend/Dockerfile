FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew --no-daemon installDist

FROM eclipse-temurin:17-jre

RUN useradd --system --create-home --uid 10001 nexa

WORKDIR /opt/nexa

COPY --from=build /workspace/build/install/nexa-finance-backend/ ./

RUN chown -R 10001:10001 /opt/nexa

USER 10001:10001

ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["/opt/nexa/bin/nexa-finance-backend"]
