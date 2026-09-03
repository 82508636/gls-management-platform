FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY libs/platform-security/pom.xml libs/platform-security/pom.xml
COPY libs/platform-security/src libs/platform-security/src
RUN mvn -q -f libs/platform-security/pom.xml -DskipTests install
COPY services/workforce-service/pom.xml services/workforce-service/pom.xml
RUN mvn -q -f services/workforce-service/pom.xml -DskipTests dependency:go-offline
COPY services/workforce-service/src services/workforce-service/src
RUN mvn -q -f services/workforce-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/workforce-service/target/workforce-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
