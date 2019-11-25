FROM adoptopenjdk:11-jre-hotspot

COPY target/plan-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/src/app/

EXPOSE 27183

WORKDIR /usr/src/app
ENTRYPOINT [ \
  "java", \
  "-jar", "plan-1.0-SNAPSHOT-jar-with-dependencies.jar" \
  ]
