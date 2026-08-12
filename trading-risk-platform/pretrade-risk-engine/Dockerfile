FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /work
COPY . .
RUN mvn -pl pretrade-risk-engine -am package -DskipTests
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 ptr
USER ptr
COPY --from=build /work/pretrade-risk-engine/target/pretrade-risk-engine-*.jar /app/app.jar
EXPOSE 8090
ENTRYPOINT ["java","-XX:+UseZGC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
