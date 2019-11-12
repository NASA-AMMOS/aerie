FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY ./target/simulation*.jar /usr/src/app/simulation.jar
EXPOSE 27185
ENTRYPOINT ["java", "-jar", "/usr/src/app/simulation.jar"]
CMD ["--spring.profiles.active=default"]
