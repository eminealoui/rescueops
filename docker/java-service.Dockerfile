# syntax=docker/dockerfile:1.7
# =====================================================================
#  Image commune à TOUS les services Java RescueOps.
#  Construite depuis la racine du dépôt (le POM parent doit être visible) :
#    docker build -f docker/java-service.Dockerfile \
#      --build-arg MODULE=services/ms-hospital --build-arg PORT=8085 -t rescueops/ms-hospital .
# =====================================================================

# --- Étape 1 : compilation ---
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# Le cache Maven est partagé entre les builds de tous les services.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -q -pl "${MODULE}" -am package -DskipTests \
    && cp "${MODULE}"/target/*.jar /workspace/app.jar

# --- Étape 2 : exécution (JRE seul, utilisateur non root) ---
FROM eclipse-temurin:21-jre
ARG PORT=8080
ENV SERVER_PORT=${PORT} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system rescueops && useradd --system --gid rescueops rescueops
WORKDIR /app
COPY --from=build --chown=rescueops:rescueops /workspace/app.jar app.jar
USER rescueops
EXPOSE ${PORT}
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
  CMD curl -fs "http://localhost:${SERVER_PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
