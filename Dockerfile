FROM gradle:jdk17 as builder
WORKDIR /workspace

COPY . /workspace/

RUN chmod +x gradlew

RUN ./gradlew build --x test

RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)
RUN echo $(ls -a)

FROM openjdk:17
WORKDIR /workspace
ENV TZ=Asia/Dhaka
RUN mkdir -p /var/log/teenpatti

ARG DEPENDENCY=/workspace/build/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib app/lib
COPY --from=builder ${DEPENDENCY}/META-INF app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes app
ENTRYPOINT ["java","-cp","app:app/lib/*","com/tanvir/MaxLiveSpringHomeApplication"]