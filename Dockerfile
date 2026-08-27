FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar ClamAV (clamscan + freshclam) en Alpine y descargar firmas de virus
RUN apk update && apk add --no-cache clamav clamav-libunrar \
    && freshclam

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
