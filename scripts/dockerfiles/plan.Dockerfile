FROM adoptopenjdk:11-jre-hotspot

COPY buck-out/gen/services/plan-service.jar /usr/src/app/

EXPOSE 27183

WORKDIR /usr/src/app
ENTRYPOINT [ "java", "-jar", "plan-service.jar"]
