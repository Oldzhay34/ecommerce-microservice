# ---- build ----
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci || npm install

COPY tsconfig.json tsconfig.node.json vite.config.ts ./
COPY tailwind.config.js postcss.config.js index.html ./
COPY src ./src

ENV VITE_API_GATEWAY_URL=http://localhost:8080
RUN npm run build

# ---- serve ----
FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]