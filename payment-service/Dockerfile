FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn -B -ntp clean verify

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/payment-service-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
