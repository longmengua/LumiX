# Maven build stage：只複製 server module，避免前端與本機產物影響可重現的 backend image。
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace/server

COPY server/.mvn .mvn
COPY server/mvnw server/pom.xml ./
RUN ./mvnw --batch-mode -DskipTests dependency:go-offline

COPY server/src src
RUN ./mvnw --batch-mode -DskipTests package

# Runtime stage 不帶 Maven、source 或 build cache，降低部署 image 的攻擊面。
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 lumix
WORKDIR /app
COPY --from=build /workspace/server/target/lumix-server-0.1.0-SNAPSHOT.jar app.jar
USER lumix
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
