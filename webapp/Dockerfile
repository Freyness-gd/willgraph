# Stage 1: Build
FROM node:20-alpine AS build

WORKDIR /app

# Copy all source files first (needed for quasar prepare in postinstall)
COPY . .

# Install dependencies
RUN yarn install --frozen-lockfile

# Build the Quasar app
RUN yarn build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy built files to nginx
COPY --from=build /app/dist/spa /usr/share/nginx/html

# Copy custom nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
