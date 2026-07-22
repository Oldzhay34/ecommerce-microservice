# 1. Aşama: Build (Derleme)
FROM node:20-alpine AS builder

WORKDIR /app

# Bağımlılıkları önbelleğe almak için önce package dosyalarını kopyala
COPY package.json package-lock.json* ./

# Bağımlılıkları yükle (Eğer package-lock.json kullanmıyorsanız sadece 'npm install' kullanın)
RUN npm install

# Tüm proje dosyalarını kopyala
COPY . .

# Projeyi derle (dist klasörünü oluşturur)
RUN npm run build

# 2. Aşama: Nginx ile Servis Etme
FROM nginx:alpine

# Varsayılan nginx konfigürasyonunu silip kendi ayarlarımızı ekliyoruz
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Derlenen dosyaları builder aşamasından Nginx'in yayın klasörüne kopyala
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx portunu dışa aç
EXPOSE 8080

# Sunucuyu başlat
CMD ["nginx", "-g", "daemon off;"]