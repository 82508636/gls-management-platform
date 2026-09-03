FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY libs/platform-security/pom.xml libs/platform-security/pom.xml
COPY libs/platform-security/src libs/platform-security/src
RUN mvn -q -f libs/platform-security/pom.xml -DskipTests install
COPY services/pickup-service/pom.xml services/pickup-service/pom.xml
RUN mvn -q -f services/pickup-service/pom.xml -DskipTests dependency:go-offline
COPY services/pickup-service/src services/pickup-service/src
RUN mvn -q -f services/pickup-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/pickup-service/target/pickup-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
