# Aşama 1: Build (Derleme)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Maven wrapper ve pom.xml dosyalarını kopyala
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Bağımlılıkları indir (cache avantajı için önce sadece pom.xml kopyalanır)
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Kaynak kodları kopyala ve derle (Testleri atlayarak hızlı build alırız)
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Aşama 2: Runtime (Çalıştırma)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sadece derlenmiş jar dosyasını builder aşamasından al
COPY --from=builder /app/target/*.jar app.jar

# Uygulamanın çalışacağı port
EXPOSE 8080

# Ortam değişkenleri için varsayılan değerler
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Uygulamayı başlat
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]