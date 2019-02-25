# Advanced MPSA Adaptation Services

This is a microservice that exposes functionality within the domain of the adaptation of a mission for Mission Planning. 

An adaptation describes the specific behavior of a spacecraft and exposes the available activity types that can be instantiated in the context of a plan. 

The RESTful services expose an API for interacting with an adaptation. Once an adaptation has been selected, there is an API to return the available activity type definitions. The service uses heavy reflection to provide the inners of the classes in the form of an activity. 

The services are written in Java 1.8 using the following technologies: 

- Spring Boot
- H2 (In memory DB)
- Spring Actuator



## Building an Running the microservices



The quickest way to build and run the services is using Maven to compile the applciation, run the tests and generate the .jar that will be executed. In order to resolve some dependencies you will need to copy the settings.xml file into your `~/.m2/` directory (you can create it if it doesn't already exist). Then run the following commands:

```shell
# Go to the location of the adaptation microservice
# Notice that in this level you should be able to see a pom.xml
cd {location_of_the_project}/adaptation

# Run Maven Install to build and test the service
mvn install

# Maven will generate the artifacts in the target folder 
cd {location_of_the_project}/adaptation/target

# Run the generated artifact, by default, the service will be exposed through port 27182
java -jar adaptation-0.0.1-SNAPSHOT.jar  
```



> Note: To change the default port number, update the application.properties file: 

```properties
server.port=27182

# Enabling H2 Console
spring.h2.console.enabled=true
```

## What is an Adaptation?

TBD...
