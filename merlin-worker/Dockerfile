FROM alpine:3.15 AS extractor

COPY build/distributions/*.tar /usr/src/app/server.tar
RUN mkdir /usr/src/app/extracted
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar -C extracted

FROM eclipse-temurin:18-focal

COPY --from=extractor /usr/src/app/extracted /usr/src/app

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/merlin-worker"]
