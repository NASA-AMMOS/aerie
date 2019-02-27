FROM openjdk:8-alpine
WORKDIR /usr/src/app
COPY ./target/adaptation*.jar /usr/src/app/adaptation.jar
COPY ./lib /usr/src/app/lib
RUN chmod 755 /usr/src/app/lib/*
RUN mkdir /usr/src/app/adaptation_files 
EXPOSE 27182
ENTRYPOINT ["java", "-jar", "/usr/src/app/adaptation.jar"]
CMD ["--spring.profiles.active=default"] 