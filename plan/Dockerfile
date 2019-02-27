FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY ./target/plan*.jar /usr/src/app/plan.jar
EXPOSE 27183
ENTRYPOINT ["java", "-jar", "/usr/src/app/plan.jar"]
CMD ["--spring.profiles.active=default"] 
