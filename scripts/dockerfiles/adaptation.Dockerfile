FROM adoptopenjdk:11-jre-hotspot

COPY buck-out/gen/services/adaptation-service.jar /usr/src/app/
RUN mkdir /usr/src/app/adaptation_files

EXPOSE 27182
VOLUME ["/usr/src/app/adaptation_files"]

WORKDIR /usr/src/app
ENTRYPOINT ["java", "-Xmx2g", "-jar", "adaptation-service.jar"]
