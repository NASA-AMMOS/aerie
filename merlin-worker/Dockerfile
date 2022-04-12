FROM alpine:3.15 AS extractor

COPY build/distributions/*.tar /usr/src/app/server.tar
RUN mkdir /usr/src/app/extracted
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar -C extracted

FROM eclipse-temurin:17-jre-focal

ARG IMAGE_INCLUDE_JDK=false
RUN if [ $IMAGE_INCLUDE_JDK = "true" ] ; then \
        apt update && apt install openjdk-17-jdk-headless -y; \
    fi ;

COPY --from=extractor /usr/src/app/extracted /usr/src/app

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/merlin-worker"]
