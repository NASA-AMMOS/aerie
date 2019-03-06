# ActiveMQ example

> Demonstrates very basic integration of ActiveMQ and Spring Boot, based on
> the [External Apache ActiveMQ Spring Boot Example][L1]

## Usage

Start ActiveMQ using Docker:

```bash
docker run -p 61616:61616 -p 5672:5672 -p 8161:8161  rmohr/activemq:5.15.6-alpine
```

Then start this example. Go to http://localhost:8080/publish/hello to publish a
a message. Anything you pass after `publish/` will be published as a message.

To verify that the message has been published, open the admin UI for ActiveMQ
http://localhost:8161 click "Manage ActiveMQ broker", and then click Queues in
the main navigation. You should see browse to Topics. You should see a queue
called `example-queue`, and it should have one consumer, one message enqueued,
and one message dequeued.


[L1]: https://www.onlinetutorialspoint.com/spring-boot/external-apache-activemq-spring-boot-example.html