# Stage 1: Build
FROM gradle:jdk17 AS builder
WORKDIR /workspace
ENV GRADLE_USER_HOME=/home/gradle/.gradle

# Copy Gradle wrapper and config first to cache dependencies
COPY build.gradle settings.gradle gradlew /workspace/
COPY gradle /workspace/gradle
RUN ./gradlew build -x test --no-daemon || return 0

# Copy source code and build
COPY . /workspace
RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /workspace
ENV TZ=Asia/Dhaka

# Create log directory
RUN mkdir -p /var/log/max-live-spring-home

# Copy the fat JAR from builder
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Run the application
ENTRYPOINT ["java","-Xms1g","-Xmx4g","-jar","app.jar"]
