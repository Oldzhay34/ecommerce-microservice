# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# pom.xml ve src klasörünü bulunduğun dizinden direkt kopyala
COPY pom.xml .
COPY src ./src

# Projeyi derle (Multi-module parametreleri çıkarıldı)
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik için non-root kullanıcı oluştur
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Derlenen jar dosyasını root target klasöründen kopyala
COPY --from=builder /build/target/*.jar app.jar

# Spring Boot varsayılan portu
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]