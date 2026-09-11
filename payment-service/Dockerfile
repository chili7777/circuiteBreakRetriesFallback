FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/payment-service-1.0.0.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","/app/app.jar"]
