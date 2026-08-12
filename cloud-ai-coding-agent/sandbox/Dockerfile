FROM eclipse-temurin:21-jdk
RUN apt-get update && apt-get install -y --no-install-recommends git curl ca-certificates nodejs npm maven && rm -rf /var/lib/apt/lists/* && useradd -m -u 10001 agent && mkdir /workspace && chown agent:agent /workspace
USER 10001
WORKDIR /workspace
