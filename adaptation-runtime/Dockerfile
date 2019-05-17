FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY ./target/adaptation-runtime*.jar /usr/src/app/adaptation-runtime.jar
RUN mkdir /usr/src/app/adaptation_files
ENTRYPOINT ["java", "-jar", "/usr/src/app/adaptation-runtime.jar"]
CMD ["--spring.profiles.active=default"]
