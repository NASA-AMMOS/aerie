FROM adoptopenjdk:11-jre-hotspot

COPY plan-service/build/distributions/*.tar /usr/src/app/service.tar
RUN cd /usr/src/app && tar --strip-components 1 -xf service.tar

EXPOSE 27183

WORKDIR /usr/src/app
ENTRYPOINT ["/usr/src/app/bin/plan-service"]
