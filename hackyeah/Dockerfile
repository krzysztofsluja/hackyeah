# Multi-stage: build inside the image, so running the demo needs Docker only - no local Java 25.
#   docker build -t hackyeah .
#   docker run --rm -p 8090:8090 hackyeah
# The database is in-memory H2 seeded on startup, so every container starts with the demo scenario.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# The cache mount keeps ~/.m2 between local rebuilds; tests run in the IDE/CI, not in the image.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home hackyeah
COPY --from=build /app/app.jar app.jar
USER hackyeah
# Rush-hour multiplier depends on local time; the image would otherwise run in UTC.
ENV TZ=Europe/Warsaw
EXPOSE 8090
ENTRYPOINT ["java", "-Duser.timezone=Europe/Warsaw", "-jar", "app.jar"]
