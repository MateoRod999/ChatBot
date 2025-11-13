
FROM eclipse-temurin:21-jre-focal

WORKDIR /app

COPY target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]