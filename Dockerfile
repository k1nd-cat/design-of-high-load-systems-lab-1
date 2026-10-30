FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B package
RUN mvn -B dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=target/dependency


FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/lab1-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./lib

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]