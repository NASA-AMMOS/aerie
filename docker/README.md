# Docker Compose

These configs include everything that is required to run AMPSA
with [Docker][docker] and [Docker Compose][compose].

> Compose is a tool for defining and running multi-container Docker 
> applications. With Compose, you use a Compose file to configure your 
> application’s services. Then, using a single command, you create and start
> all the services from your configuration.

In this configuration, Docker Compose will pull down pre-built Docker
containers for each of the servers which make up the AMPSA application.
These containers are built at runtime, but will eventulally be retrieved
from our local JPL installation of DockerHub which is hosted on
[Artifactory][artifactory].

Once the necessary containers are pulled down, Docker Compose will orchestrate
the network of services and start them up.

## IMPORTANT NOTE

This demo is NOT designed for production use or performance
testing. It has not been optimized in any way. It WILL be slow and should not be
considered for release. Per Tyk recommendations, a production configuration
would have each component on a separate machine, following the [docs][tyk],
however the actual configuration is TBD.

## Configuration

The basic command that you will use to start services with Docker Compse is:

    docker-compose up

But this would only start the default service, and there isn't one intentionally.
To start additional services, you will layer on additional configuration files
like this:

    docker-compose [-f <feature>.yml] up -d

The `<feature>.yml` corresponds to additional features
that you may require, as outlined below. The `-d` option will run the services
in the background. To get a feel for how the service works though, you can
omit it.

The services will take several minutes to start. Once the logging output from
`docker-compose` has slowed down, you should be able to access the services as
per their individual documentation.

Press `ctrl+c` to stop all services if docker-compose is running in the
foreground. Or to stop an application which has been started in the backgorund,
run the following:

    docker-compose [-f <feature>.yml] stop 

Where the `<feature>.yml` arguments correspond to the
additional features which you started the application with.

### Base configuration 

To start any service you will need to run `docker-compose` with the base
configuration.

    docker-compose -f base.yml up 

### NEST configuration

To start the NEST service you will use the `nest` file:

    docker-compose -f base.yml -f nest.yml up 

### Adaptation configuration

To start the Adaptation service you will use the `adaptation` file:

    docker-compose -f base.yml -f adaptation.yml up

### Logging configuration

To start the log stack with Elasticsearch, Kibana, and Filebeat, you will use
the `elastic` file:

    docker-compose -f base -f elastic.yml up

### Tyk configuration

To start the Tyk stack you will first need to do some minor configuration.
Then you can use the `tyk` and `tyk_local` files. See step 3, below.

1. Set up your `/etc/hosts` file to include the IP of your docker daemon:

```
127.0.0.1 www.tyk-portal-test.com
127.0.0.1 www.tyk-test.com
```

Note that the IP may be different depending on your installation, Windows users 
may find it running on `10.x.x.x`, it is important the URL stays the same 
because our `setup.sh` assumes this is the one you are using.

2. Add your dashboard license

Open the `../tyk_dashboard/tyk_dashboard.conf` file and add your license string
to the `"license_key": ""` section.

3. Start the Tyk stack locally

```
docker-compose -f base.yml -f tyk.yml -f tyk_local.yml up
```

4. Bootstrap the instance (creates a test user):

```
cd ../tyk_dashboard
chmod +x setup.sh 
./setup.sh 
```

Make sure you save the passwords somewhere.

5. Log in with the credentials provided.

The setup script will provide a username and password, as well as the URL of 
your portal, please note that this will be running on port 3000, not port 80.

### Run all services together

This is a long, but very composable command:

    docker-compose \
      -f base.yml \
      -f nest.yml \
      -f adaptation.yml \
      -f elastic.yml \
      -f tyk.yml \
      -f tyk_local.yml \
      up -d

Be patient, it can take up to 15 minutes to fully start every service.

[artifactory]: https://cae-artifactory.jpl.nasa.gov
[tyk]: https://tyk.io/docs/.
