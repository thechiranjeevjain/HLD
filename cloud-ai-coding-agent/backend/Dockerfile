FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -B -pl backend -am dependency:go-offline
COPY backend ./backend
RUN mvn -B -pl backend -am package -DskipTests
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl docker.io && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home agent
USER 10001
COPY --from=build /src/backend/target/cloud-ai-coding-agent-backend-*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
