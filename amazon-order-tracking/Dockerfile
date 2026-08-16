FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q clean package
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 tracking
WORKDIR /app
COPY --from=build /workspace/target/amazon-order-tracking-1.0.0.jar app.jar
USER tracking
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
