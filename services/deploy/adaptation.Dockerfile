FROM adoptopenjdk:11-jre-hotspot

COPY target/adaptation-service-jar-with-dependencies.jar /usr/src/app/
RUN mkdir /usr/src/app/adaptation_files

EXPOSE 27182
VOLUME ["/usr/src/app/adaptation_files"]

WORKDIR /usr/src/app
ENTRYPOINT [ \
  "java", \
  "-jar", "adaptation-service-jar-with-dependencies.jar" \
  ]
