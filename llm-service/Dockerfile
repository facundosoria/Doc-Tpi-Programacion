FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . ./
RUN mvn -q -pl app -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/app/target/llm-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
