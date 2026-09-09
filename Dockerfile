# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o POM primeiro para aproveitar cache de dependências.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
# Imagem mínima: somente JRE. O apk upgrade aplica correções de segurança
# disponíveis para bibliotecas do Alpine (ex.: OpenSSL) antes da execução.
FROM eclipse-temurin:17-jre-alpine
USER root
RUN apk upgrade --no-cache \
    && addgroup -S -g 1001 spring \
    && adduser -S -D -H -u 1001 -G spring spring

WORKDIR /app
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Aplicação nunca executa como root.
USER 1001:1001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
