# 1. Aşama: Build süreci
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2. Aşama: Nginx ile servis etme süreci
FROM nginx:alpine
# Build klasörünü nginx'in servis edeceği yere kopyala
COPY --from=build /app/dist /usr/share/nginx/html
# Nginx ayarını kopyala
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]