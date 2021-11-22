FROM artifactory.jpl.nasa.gov:17001/eclipse-temurin:17-focal

COPY scheduler-server/build/distributions/*.tar /usr/src/app/server.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf server.tar
RUN mkdir -p /usr/src/app/merlin_file_store

EXPOSE 27193
VOLUME ["/usr/src/app/merlin_file_store"]

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/scheduler-server"]
