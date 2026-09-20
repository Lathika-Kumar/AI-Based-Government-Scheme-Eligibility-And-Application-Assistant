# ── Stage 1: Build JAR with Maven ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy POM and source code
COPY pom.xml .
COPY src ./src

# Build production executable JAR (skipping unit tests during image build)
RUN mvn clean package -DskipTests

# ── Stage 2: Lightweight Production JRE Runtime ───────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-privileged user for security
RUN addgroup -S schemebridge && adduser -S sbscheme -G schemebridge
USER sbscheme:schemebridge

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# JVM options optimized for container memory constraints
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8081}/actuator/health || exit 0

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8081} -jar app.jar"]
