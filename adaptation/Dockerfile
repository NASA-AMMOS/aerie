FROM adoptopenjdk:11-jre-hotspot
WORKDIR /usr/src/app
COPY ./target/adaptation*.jar /usr/src/app/adaptation.jar
RUN mkdir /usr/src/app/adaptation_files 
EXPOSE 27182
ENTRYPOINT ["java", "-jar", "/usr/src/app/adaptation.jar"]
CMD ["--spring.profiles.active=default"]
