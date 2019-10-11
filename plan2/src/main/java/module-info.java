module gov.nasa.jpl.ammos.mpsa.aerie.plan {
  requires gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;
  requires io.javalin;
  requires java.json;
  requires java.json.bind;
  requires java.net.http;
  requires org.apache.commons.lang3;
  requires org.mongodb.bson;
  requires org.mongodb.driver.core;
  requires org.mongodb.driver.sync.client;
  requires slf4j.api;

  uses javax.json.spi.JsonProvider;

  exports gov.nasa.jpl.ammos.mpsa.aerie.plan.models;
}
