FROM gradle:jdk17 AS builder
WORKDIR /workspace

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY . /workspace/

RUN chmod +x gradlew

#RUN gradle clean build -x test
RUN ./gradlew clean build -x test --no-daemon --refresh-dependencies

RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)
RUN echo $(ls -a)

# Stage 2: Runtime
FROM openjdk:17
WORKDIR /workspace
ENV TZ=Asia/Dhaka
RUN mkdir -p /var/log/max-live-spring-home

ARG DEPENDENCY=/workspace/build/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib app/lib
COPY --from=builder ${DEPENDENCY}/META-INF app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes app
ENTRYPOINT ["java","-Xms1g","-Xmx4g","-cp","app:app/lib/*","com/tanvir/MaxLiveSpringHomeApplication"]