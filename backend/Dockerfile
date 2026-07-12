# ==========================================================
# Estágio 1 — Build (Maven + JDK 21)
# ==========================================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para aproveitar o cache de camadas:
# as dependências só são baixadas novamente se o pom.xml mudar.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ==========================================================
# Estágio 2 — Runtime (JRE 21, imagem mínima)
# ==========================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Diretório de uploads (STL/3MF, fotos) com permissão para o usuário da aplicação
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
