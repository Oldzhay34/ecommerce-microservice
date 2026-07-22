# Aşama 1: Bağımlılıkların yüklenmesi ve derleme (Build)
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# Aşama 2: Üretim (Production) sunucusu
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 5001

CMD ["nginx", "-g", "daemon off;"]