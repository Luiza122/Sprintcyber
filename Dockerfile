# Build stage — usa Maven+JDK completos só aqui, nunca vão pra imagem final
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage — imagem mínima (só JRE, sem Maven/JDK/shell desnecessário),
# reduz superfície de ataque e tamanho da imagem.
FROM eclipse-temurin:17-jre-alpine

# Nunca roda a aplicação como root dentro do container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
