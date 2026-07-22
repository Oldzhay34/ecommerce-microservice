# ==========================================
# Aşama 1: Derleme (Build Stage)
# ==========================================
FROM node:20-alpine AS builder

WORKDIR /app

# Sadece bağımlılık dosyalarını kopyala (Docker cache'ini optimize etmek için)
COPY package.json package-lock.json* ./

# Bağımlılıkları kur
RUN npm ci

# Tüm kaynak kodunu kopyala
COPY . .

# Vite, environment değişkenlerini build anında index.html ve js içine gömer.
# Bu yüzden build anında bu ARG'ları alıp ENV olarak ayarlıyoruz.
ARG VITE_API_GATEWAY_URL
ENV VITE_API_GATEWAY_URL=$VITE_API_GATEWAY_URL

# Projeyi derle (dist klasörü oluşacak)
RUN npm run build

# ==========================================
# Aşama 2: Sunum (Production Stage)
# ==========================================
FROM nginx:alpine

# Varsayılan Nginx konfigürasyonunu sil ve bizimkini kopyala
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Builder aşamasından derlenmiş statik dosyaları Nginx HTML dizinine aktar
COPY --from=builder /app/dist /usr/share/nginx/html

# Uygulamanın çalışacağı port (Vite config'deki ile aynı)
EXPOSE 6005

# Nginx'i başlat
CMD ["nginx", "-g", "daemon off;"]