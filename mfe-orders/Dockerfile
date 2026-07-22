# ==========================================
# Aşama 1: Node ile Derleme (Builder)
# ==========================================
FROM node:20-alpine AS builder

WORKDIR /app

# Docker önbelleğini efektif kullanmak için sadece paket dosyalarını kopyalıyoruz
COPY package.json package-lock.json* ./

# Bağımlılıkları kur
RUN npm install

# Kaynak kodun geri kalanını kopyala
COPY . .

# Varsa çevresel değişkenlerinizi (Gateway URL vs.) burada build argümanı olarak alabilirsiniz
ARG VITE_API_GATEWAY_URL
ENV VITE_API_GATEWAY_URL=$VITE_API_GATEWAY_URL

# Projeyi derle
RUN npm run build

# ==========================================
# Aşama 2: Nginx ile Sunum (Production)
# ==========================================
FROM nginx:alpine

# Nginx'in varsayılan ayarını silip kendi yapılandırmamızı yüklüyoruz
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Builder aşamasında üretilen dist klasörünü Nginx'e kopyala
COPY --from=builder /app/dist /usr/share/nginx/html

# vite.config.ts'de belirttiğiniz 6001 portunu dışa açıyoruz
EXPOSE 6001

CMD ["nginx", "-g", "daemon off;"]