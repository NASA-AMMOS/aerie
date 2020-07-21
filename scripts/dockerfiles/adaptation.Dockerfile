FROM adoptopenjdk:11-jre-hotspot

COPY adaptation-service/build/distributions/*.tar /usr/src/app/service.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf service.tar
RUN mkdir -p /usr/src/app/adaptation_files

EXPOSE 27182
VOLUME ["/usr/src/app/adaptation_files"]

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/adaptation-service"]
