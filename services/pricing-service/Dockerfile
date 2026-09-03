FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY libs/platform-security/pom.xml libs/platform-security/pom.xml
COPY libs/platform-security/src libs/platform-security/src
RUN mvn -q -f libs/platform-security/pom.xml -DskipTests install
COPY services/pricing-service/pom.xml services/pricing-service/pom.xml
RUN mvn -q -f services/pricing-service/pom.xml -DskipTests dependency:go-offline
COPY services/pricing-service/src services/pricing-service/src
RUN mvn -q -f services/pricing-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/pricing-service/target/pricing-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
