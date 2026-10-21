FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN sh gradlew :warehouse-service:bootJar

FROM eclipse-temurin:25-jre
COPY --from=build /app/warehouse-service/build/libs/warehouse-service.jar /app.jar
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app.jar"]
