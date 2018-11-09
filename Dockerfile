# This could be a multi-part build which would use a node container to build
# the app and copy it into the final build which uses nginx as its base.
# Note that we intentionally avoid package-lock.json to prevent the error:
# `npm code EINTEGRITY integrity checksum failed when using sha512`

# FROM node:10-alpine
# WORKDIR /usr/src/app
# COPY package.json ./
# RUN npm install -g @angular/cli	
# RUN npm install
# COPY . .
# RUN npm run build-prod 

# For now, assume that the project has already been built and exists in the
# `dist` folder on the host machine. Then create the nginx container
# which will run NEST
FROM nginx
COPY dist /usr/share/nginx/html
