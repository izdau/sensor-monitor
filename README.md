# Sensor Monitor

A small event-driven system that watches the temperature and humidity sensors of one or more warehouses.

- **warehouse-service** receives sensor measurements over UDP and publishes them to Kafka.
- **central-service** consumes the measurements from Kafka and raises alarms in its log
  when a configured threshold is exceeded.

Java 25, Spring Boot, reactive with Reactor (Flux/Mono) on Reactor Netty, Gradle, Apache Kafka.

## Compile and test

| Check | CMD | Git Bash |
|---|---|---|
| Compile only (fast, good after renames) | `gradlew.bat compileJava compileTestJava` | `./gradlew compileJava compileTestJava` |
| Full check: compile, tests, jars | `gradlew.bat clean build` | `./gradlew clean build` |
