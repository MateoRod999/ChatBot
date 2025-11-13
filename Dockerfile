# --- STAGE 1: COMPILACIÓN (BUILDER) ---
# Usamos una imagen que tiene Java 21 y Maven preinstalados.
FROM maven:3.9.5-openjdk-21 AS builder


COPY . /app
WORKDIR /app

RUN mvn clean package -Dmaven.test.skip=true


FROM eclipse-temurin:21


WORKDIR /app

COPY --from=builder /app/target/ChatBot-0.0.1-SNAPSHOT.jar app.jar

# Puerto de escucha
EXPOSE 8080

# Comando para ejecutar el bot
ENTRYPOINT ["java", "-jar", "app.jar"]