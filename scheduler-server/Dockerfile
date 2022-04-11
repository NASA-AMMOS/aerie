FROM alpine:3.15 AS extractor

COPY build/distributions/*.tar /usr/src/app/server.tar
RUN mkdir /usr/src/app/extracted
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar -C extracted

FROM eclipse-temurin:17-jre-focal

ENV NODE_VERSION=16.14.0
RUN apt install -y curl
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN node --version
RUN npm --version

COPY --from=extractor /usr/src/app/extracted /usr/src/app

COPY scheduling-dsl-compiler /usr/src/app/scheduling-dsl-compiler
ENV SCHEDULING_DSL_COMPILER_ROOT="/usr/src/app/scheduling-dsl-compiler/"
ENV SCHEDULING_DSL_COMPILER_COMMAND="./build/main.js"
ENV NODE_PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/node"

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/scheduler-server"]
