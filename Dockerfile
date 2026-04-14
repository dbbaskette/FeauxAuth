FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build frontend and backend
COPY frontend/ frontend/
COPY src/ src/

# Harden npm against transient registry network errors (ECONNRESET, TLS drops)
ENV NPM_CONFIG_FETCH_RETRIES=5 \
    NPM_CONFIG_FETCH_RETRY_MINTIMEOUT=20000 \
    NPM_CONFIG_FETCH_RETRY_MAXTIMEOUT=120000 \
    NPM_CONFIG_FETCH_TIMEOUT=300000

RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    for i in 1 2 3; do \
      mvn clean package -DskipTests -B && break || \
      { echo "Build attempt $i failed, retrying..."; sleep 10; }; \
    done

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/feauxauth-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
