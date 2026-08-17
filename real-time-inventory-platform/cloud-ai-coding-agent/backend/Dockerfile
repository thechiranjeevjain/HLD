FROM gradle:8.13-jdk21 AS build
WORKDIR /src
COPY settings.gradle build.gradle ./
COPY backend ./backend
RUN gradle :backend:bootJar --no-daemon
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl docker.io && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home agent
USER 10001
COPY --from=build /src/backend/build/libs/*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
