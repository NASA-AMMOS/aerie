# Single Host Deployment

Installation instructions are located in the Aerie repository [deployment documentation](https://github.com/NASA-AMMOS/aerie/blob/develop/deployment). There you can also find an example [docker-compose.yml](https://github.com/NASA-AMMOS/aerie/blob/develop/deployment/docker-compose.yml) file.

## Docker Images

Aerie Docker images are available at [GitHub packages](https://github.com/orgs/NASA-AMMOS/packages?tab=packages&ecosystem=container&q=aerie). 

## Known Issues

1. Using a custom base URL path for Aerie UI 
    * Follow the instructions linked at [Custom Base Path](ui-custom-base-path) to deploy Aerie UI at a custom base path.
2. Annotations processing being used for Activity Mapping
    * When using the IntelliJ IDE, upon a source file change, only the affected source files will be recompiled. This causes conflicts with the annotations processing being used for Activity Mapping. For now manually rebuilding every time is the solution.

## Administration

This product is using Docker containers to run the application. The Docker containers are internally bridged (connected) to run the application. Containers can be restarted in case of any issues using Docker CLI. See the [TCP Port Requirements](system-requirements.md#tcp-port-requirements) for which containers should be exposed publicly/outside the Docker network.

## Configuration

Each Aerie service is configured with environment variables. A description of those variables is found in the [Environment Variable Documentation](https://github.com/NASA-AMMOS/aerie/blob/develop/deployment/Environment.md)

Of note, the aerie-merlin, aerie-scheduler, and aerie_merlin_worker(s) can be provided additional JVM arguments as environment variables. For example one may choose to configure the JVM allocated heap size. On must provide any desired JVM flags to the `JAVA_OPTS` environment variable for the container being configured.

## Network Communications

The Aerie deployment configures the port numbers for each container via docker-compose. The port numbers must match those declared within the services' config.json. In a large majority of Aerie deployments no change to these port numbers will be needed, nor should one be made. The only port number that might be desired to change is the Aerie-UI port (80). In this case the number to change is the first port number of the pair [XXXX:XXXX]. The second number represents the port number within the container itself.

## Administration Procedures

Aerie employs an orchestrated containerized architecture. Each of the software components are independently packaged and run in a container. 
- aerie-commanding: Provides activity to command expansion authoring and processing. 
- aerie-gateway: Main API gateway for Aerie
- aerie-merlin: Handles all the logic and functionality for activity planning.
- aerie-merlin-worker: Runs simulation jobs requested of the aerie-merlin service.
- aerie-scheduler: Provides automated activity plan scheduling. 
- aerie-ui: Hosts the web application and communicates with Aerie via the GraphQL Apollo Server.
- postgres: Holds the data for the Merlin server container. This container is optional and included in many of the aerie docker compose files for convenience. Long term deployments will want to integrate a Postgres database not running within a container. 
- hasura: Serves the Aerie GraphQL API.

Aerie database containers are isolated and connect only with service containers internal to the application (Merlin server, UI server, Aerie gateway server). Aerie databases are not (and should not be made to be) accessible from outside the Aerie application. 

## Network File System Deployment

You can install and use a Network File System with Aerie. Please see the [deployment documentation](https://github.com/NASA-AMMOS/aerie/blob/develop/deployment/NFS.md) for complete instructions.
