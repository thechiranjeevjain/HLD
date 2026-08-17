FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY history-service/pom.xml history-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY risk-service/pom.xml risk-service/pom.xml
RUN mvn -B -q -pl api-gateway -am dependency:go-offline
COPY shared shared
COPY api-gateway api-gateway
RUN mvn -B -q -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENV SERVER_PORT=8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
