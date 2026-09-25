# Multi-stage build for profilescreener-api (bare-minimum demo image)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN mkdir -p /app/resumes && chown -R app:app /app
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
