# ---------- Étape 1 : compilation avec Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Étape 2 : image d'exécution légère ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
RUN groupadd --system smartcampus && useradd --system --gid smartcampus smartcampus
COPY --from=build --chown=smartcampus:smartcampus /app/target/smartcampus-*.jar app.jar
USER smartcampus
EXPOSE 8090
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fs http://localhost:8090/api/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
