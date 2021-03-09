FROM adoptopenjdk:11-jre-hotspot

COPY merlin-server/build/distributions/*.tar /usr/src/app/server.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar
RUN mkdir -p /usr/src/app/adaptation_files

EXPOSE 27183
VOLUME ["/usr/src/app/adaptation_files"]

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/merlin-server"]
