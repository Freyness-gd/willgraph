FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .

COPY settings.gradle .

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src src

COPY config config
RUN ./gradlew bootJar -x test --no-daemon

EXPOSE 8080

ENTRYPOINT ["java","-jar","build/libs/willgraph-0.0.1-SNAPSHOT.jar"]