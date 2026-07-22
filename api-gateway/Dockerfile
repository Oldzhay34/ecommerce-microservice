# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Bagimliliklari onbellekle
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Kaynaklari kopyala ve paketle
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Guvenlik: root olmayan kullanici
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /build/target/api-gateway-1.0.0.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
