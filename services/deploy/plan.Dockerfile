FROM adoptopenjdk:11-jre-hotspot

COPY target/plan-service-jar-with-dependencies.jar /usr/src/app/

EXPOSE 27183

WORKDIR /usr/src/app
ENTRYPOINT [ \
  "java", \
  "-jar", "plan-service-jar-with-dependencies.jar" \
  ]
