# Built by .github/workflows/deploy.yml (context ., file Dockerfile) and pushed
# to Artifact Registry.
#
# A job image, not a server: the default command runs the Spark job in local
# mode (local[*], inside this one JVM) and exits 0 on success. It will never
# satisfy a $PORT health check. Two stages: Maven builds and tests
# target/app.jar plus its runtime jars in target/lib (the jar's manifest names
# them, and carries the Add-Opens Spark needs on Java 17+), a JRE-only image
# runs it as a non-root user with a real home (Hadoop looks the user up).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

FROM eclipse-temurin:21-jre AS runtime
ARG BUILD_ID=""
WORKDIR /app
ENV BUILD_ID=$BUILD_ID
RUN useradd -m -u 10001 app
COPY --from=build /src/target/lib /app/lib
COPY --from=build /src/target/app.jar /app/app.jar
USER app
CMD ["java", "-jar", "/app/app.jar"]
