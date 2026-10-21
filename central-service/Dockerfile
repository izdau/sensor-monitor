FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN sh gradlew :central-service:bootJar

FROM eclipse-temurin:25-jre
COPY --from=build /app/central-service/build/libs/central-service.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
