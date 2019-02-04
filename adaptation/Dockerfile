# This could be a multi-part build which would use a maven container to build
# the app and copy it into the final build which uses openjdk as its base.
# But there is an issue with cae-artifactory.jpl.nasa.gov being unavailable.

# FROM maven:3.5-jdk-8 AS MAVEN_TOOL_CHAIN
# WORKDIR /tmp/
# COPY settings.xml /usr/share/maven/ref/ 
# COPY . .
# RUN mvn -U -B -s /usr/share/maven/ref/settings.xml clean install


# For now, assume that the project has already been built and exists in the
# `target` folder on the host machine. Then create the openjdk container
# which will run the adaptation service
FROM openjdk:8-alpine
WORKDIR /usr/src/adaptation
COPY ./target/adaptation*.jar /usr/src/adaptation/adaptation.jar
COPY ./lib /usr/src/adaptation/lib
RUN chmod 755 /usr/src/adaptation/lib/*
RUN mkdir /usr/src/adaptation/adaptation_files 
VOLUME [ "/usr/src/adaptation/adaptation_files" ]
EXPOSE 27182
CMD ["java", "-jar", "-Dspring.profiles.active=default", "adaptation.jar"]
