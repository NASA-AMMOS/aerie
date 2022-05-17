FROM alpine:3.15 AS extractor

COPY build/distributions/*.tar /usr/src/app/server.tar
RUN mkdir /usr/src/app/extracted
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar -C extracted

FROM eclipse-temurin:18-focal

ENV NODE_VERSION=16.14.0
ENV NVM_DIR=/root/.nvm
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN apt install --no-install-recommends -y curl \
    && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash \
    &&. "$NVM_DIR/nvm.sh" \
    && nvm install -b ${NODE_VERSION} \
    && nvm use v${NODE_VERSION} \
    && nvm alias default v${NODE_VERSION} \
    && node --version \
    && npm --version

COPY --from=extractor /usr/src/app/extracted /usr/src/app

COPY constraints-dsl-compiler /usr/src/app/constraints-dsl-compiler
ENV CONSTRAINTS_DSL_COMPILER_ROOT="/usr/src/app/constraints-dsl-compiler/"
ENV CONSTRAINTS_DSL_COMPILER_COMMAND="./build/main.js"
ENV NODE_PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/node"

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/merlin-server"]
