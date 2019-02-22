# This is a multi-part build which would use a maven container to build
# the app and copy it into the final build which uses openjdk as its base.

FROM maven:3.5-jdk-8 AS MAVEN_TOOL_CHAIN
WORKDIR /tmp/
COPY pom.xml .
COPY settings.xml /usr/share/maven/ref/ 
COPY ./lib ./lib
RUN chmod 755 ./lib/*
RUN pwd && ls -lah lib
RUN mvn -B -f pom.xml -s /usr/share/maven/ref/settings.xml dependency:resolve
COPY . .
RUN mvn -B -s /usr/share/maven/ref/settings.xml package -DskipTests

# Get the built jar from the intermediate container's `target` folder.
# Then create the openjdk container which will run the adaptation service

FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/adaptation*.jar /usr/src/app/adaptation.jar
COPY ./lib /usr/src/app/lib
RUN chmod 755 /usr/src/app/lib/*
RUN mkdir /usr/src/app/adaptation_files 
EXPOSE 27182
ENTRYPOINT ["java", "-jar", "/usr/src/app/adaptation.jar"]
CMD ["--spring.profiles.active=default"] 