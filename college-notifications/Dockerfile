# Multi-stage build for React college-notifications app
FROM node:18-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files for dependency caching
COPY package*.json ./

# Install dependencies
RUN npm install --production && npm cache clean --force

# Copy source code
COPY public/ public/
COPY src/ src/

# Build the application
RUN npm run build

# Final stage - nginx
FROM nginx:alpine

# Copy custom nginx config
COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 8080;
    server_name localhost;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files \$uri \$uri/ /index.html;
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
EOF

# Copy build files from builder stage
COPY --from=builder /app/build /usr/share/nginx/html

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1
CMD ["nginx", "-g", "daemon off;"]