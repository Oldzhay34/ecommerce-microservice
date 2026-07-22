# Aşama 1: Bağımlılıkların yüklenmesi ve uygulamanın derlenmesi (Build)
FROM node:20-alpine AS builder

WORKDIR /app

# Sadece package dosyalarını kopyalayarak Docker önbelleğini (cache) verimli kullanıyoruz
COPY package.json package-lock.json* ./

# Bağımlılıkları yüklüyoruz
RUN npm install

# Tüm kaynak kodunu kopyalayıp build alıyoruz
COPY . .
RUN npm run build

# Aşama 2: Üretim (Production) sunucusu
FROM nginx:alpine

# Hazırladığımız özel Nginx konfigürasyonunu içeri aktarıyoruz
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Builder aşamasındaki derlenmiş dosyaları Nginx'in sunacağı klasöre kopyalıyoruz
COPY --from=builder /app/dist /usr/share/nginx/html

# Vite config'de belirttiğin 5000 portunu dışa açıyoruz
EXPOSE 5000

# Nginx'i başlatıyoruz
CMD ["nginx", "-g", "daemon off;"]