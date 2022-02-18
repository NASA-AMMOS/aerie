FROM eclipse-temurin:17-focal

COPY build/distributions/*.tar /usr/src/app/worker.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf worker.tar

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/merlin-worker"]
