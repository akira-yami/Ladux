# syntax=docker/dockerfile:1
FROM node:24-alpine AS builder

WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_RECAPTCHA_SITE_KEY=
ARG VITE_SITE_URL=
ARG VITE_ALLOW_INDEXING=false
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_RECAPTCHA_SITE_KEY=${VITE_RECAPTCHA_SITE_KEY} \
    VITE_SITE_URL=${VITE_SITE_URL} \
    VITE_ALLOW_INDEXING=${VITE_ALLOW_INDEXING} \
    VITE_ENABLE_UNSAFE_ORDER_RETURN_TRANSITIONS=false
RUN npm run build

FROM nginx:stable-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
