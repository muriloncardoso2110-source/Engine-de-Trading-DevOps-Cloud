FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -B test package
FROM eclipse-temurin:21-jre
RUN useradd -r -u 10001 aurora
WORKDIR /app
COPY --from=build /build/target/aurora-exchange-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxGCPauseMillis=10 -Xms256m -Xmx512m"
ENTRYPOINT ["java","-jar","/app/app.jar"]
