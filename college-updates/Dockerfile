# Multi-stage build for Angular college-updates app
FROM node:20-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files for dependency caching
COPY package*.json ./

# Install dependencies including Angular CLI
RUN npm install && \
    npm install -g @angular/cli@latest && \
    npm cache clean --force

# Copy source code - pass --build-arg CACHEBUST=$(date +%s) to force re-copy
ARG CACHEBUST=1
COPY . .

# Build the application for production
RUN ng build --configuration=production

# Final stage - nginx
FROM nginx:alpine

# Copy custom nginx config for Angular
COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 8080;
    server_name localhost;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files \$uri \$uri/ /index.html;
    }

    # Enable gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
EOF

# Copy build files from builder stage (Angular builds to dist/ by default)
COPY --from=builder /app/dist/college-updates/browser /usr/share/nginx/html

# nginx:alpine already ships with the nginx user — no need to recreate it

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
