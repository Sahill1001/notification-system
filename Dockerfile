FROM maven:3.9.9-eclipse-temurin-21 AS api-build
WORKDIR /app

COPY notification-api/pom.xml .
COPY notification-api/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS api
WORKDIR /app
COPY --from=api-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]

FROM maven:3.9.9-eclipse-temurin-21 AS worker-build
WORKDIR /app

COPY notification-worker/pom.xml .
COPY notification-worker/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS worker
WORKDIR /app
COPY --from=worker-build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]
