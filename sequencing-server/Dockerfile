
## Use a Debian-based image for apt-get compatibility
FROM ubuntu:24.04

# Update package lists
RUN apt-get update && apt-get install --no-install-recommends  -y curl gnupg ca-certificates

# Install NodeSource repository for node 18 LTS
RUN curl -sL https://deb.nodesource.com/setup_18.x | bash -

# Update package lists again after adding NodeSource
RUN apt-get update && apt-get install --no-install-recommends -y nodejs

# Install jemalloc and set the path
RUN apt-get update && apt-get install --no-install-recommends -y libjemalloc-dev \
 && echo "/usr/lib/x86_64-linux-gnu/libjemalloc.so" >> /etc/ld.so.preload

# Set working directory (optiona
COPY . /app
WORKDIR /app
CMD [ "npm", "start" ]
