# syntax=docker/dockerfile:1

# ---------- 1) Compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
# El pom se copia primero: mientras no cambie, Docker reutiliza la capa de dependencias
# y solo recompila cuando cambia el codigo (builds mucho mas rapidos).
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests \
 && cp target/*.jar /app.jar

# ---------- 2) Imagen final: solo el JRE y el jar ----------
# Se separa del paso anterior para no llevar Maven ni el codigo fuente a produccion.
# Sin "platform:" fijo a proposito: asi funciona igual en Apple Silicon (arm64) y en x86.
FROM eclipse-temurin:21-jre-alpine
# Usuario sin privilegios: si alguien vulnera la app, no es root dentro del contenedor.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
