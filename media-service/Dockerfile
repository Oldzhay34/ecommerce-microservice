# ---------- Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

# ---------- Runtime ----------
FROM eclipse-temurin:21-jdk-alpine

# scrimage-webp native cwebp/dwebp binary'lerini paketler; alpine (musl) uzerinde
# glibc uyumlulugu icin libc6-compat ZORUNLUDUR. Ek cwebp kurulumu GEREKMEZ.
# [Varsayim 4] Farkli base image kullanilirsa bu bagimlilik yeniden dogrulanmalidir.
RUN apk add --no-cache libc6-compat

WORKDIR /app

RUN addgroup -S media && adduser -S media -G media

COPY --from=builder /build/target/media-service.jar app.jar

RUN mkdir -p /tmp/media-upload && chown -R media:media /app /tmp/media-upload

USER media

EXPOSE 8086

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]