# Stage 1: Dependencies cache
  FROM gradle:8.4-jdk17 AS deps-cache
  WORKDIR /workspace
  COPY build.gradle settings.gradle gradle.properties* /workspace/
  COPY gradle/ /workspace/gradle/
  RUN gradle dependencies --no-daemon --no-build-cache > /dev/null 2>&1 || true

  # Stage 2: Build
  FROM gradle:8.4-jdk17 AS builder
  WORKDIR /workspace
  COPY --from=deps-cache /home/gradle/.gradle /home/gradle/.gradle
  COPY build.gradle settings.gradle gradle.properties* /workspace/
  COPY gradle/ /workspace/gradle/
  COPY src/ /workspace/src/
  RUN gradle clean build -x test --no-daemon --build-cache

  # Stage 3: Runtime
  FROM eclipse-temurin:17-jre-alpine
  WORKDIR /workspace
  RUN mkdir -p /var/log/max-live-spring-home
  COPY --from=builder /workspace/build/libs/*.jar app.jar
  ENTRYPOINT ["java", "-Xms1g", "-Xmx4g", "-jar", "app.jar"]