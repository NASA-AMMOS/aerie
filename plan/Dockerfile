# This is a multi-part build which uses a maven container to build the app and
# copy it into the final build which uses openjdk as its base.

FROM maven:3.5-jdk-8 AS MAVEN_TOOL_CHAIN
WORKDIR /tmp/
COPY pom.xml .
RUN mvn -B -f pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY . .
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package -DskipTests

# Get the built jar from the intermediate container's `target` folder.
# Then create the openjdk container which will run the adaptation service

FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/plan*.jar /usr/src/app/plan.jar
EXPOSE 27183
ENTRYPOINT ["java", "-jar", "/usr/src/app/plan.jar"]
CMD ["--spring.profiles.active=default"] 
