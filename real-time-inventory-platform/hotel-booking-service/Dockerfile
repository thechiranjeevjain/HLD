FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN addgroup -S booking && adduser -S booking -G booking
COPY --from=build /workspace/target/booking-0.0.1-SNAPSHOT.jar /app/app.jar

USER booking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
