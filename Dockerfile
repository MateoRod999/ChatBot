
FROM eclipse-temurin:21

WORKDIR /app

COPY target/*.jar ChatBot-0.0.1-SNAPSHOT.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]