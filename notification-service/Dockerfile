# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# GÜNCELLEME: İsim pom.xml'deki yeni çıktıya göre düzeltildi!
COPY --from=build /app/target/notification-service-1.0.0.jar app.jar
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]