# This is a multi-part build which uses a node container to build
# the app and copy it into the final build which uses nginx as its base

FROM node:10 as nest-intermediate
WORKDIR /tmp/

# install chrome for protractor tests
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
RUN sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list'
RUN apt-get update && apt-get install -yq google-chrome-stable

COPY package*.json ./
RUN npm install
COPY . .
RUN cat src/environments/environment.ts
RUN npm run format:check
RUN npm run license:check
RUN npx ng test --code-coverage --watch=false
RUN npx ng e2e
RUN npm run build-prod
RUN tar -czf build_log.tar.gz karma-test-results.xml coverage dist

# Get the built project from the intermediate container's `dist` folder.
# Then create the nginx container which will run the service

FROM nginx
COPY --from=nest-intermediate /tmp/dist /usr/share/nginx/html
COPY --from=nest-intermediate /tmp/build_log.tar.gz /var/log/build_log.tar.gz
