ARG MODULE

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN mvn -pl "${MODULE}" -am package -DskipTests && \
    cp "${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar" /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
