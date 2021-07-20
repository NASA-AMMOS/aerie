package gov.nasa.jpl.aerie.merlin.server.config;

import java.net.URI;
import java.util.Objects;

public record MongoStore (
    URI uri,
    String database,
    String planCollection,
    String activityCollection,
    String adaptationCollection,
    String simulationResultsCollection
) implements Store {
  public MongoStore {
    Objects.requireNonNull(uri);
    Objects.requireNonNull(database);
    Objects.requireNonNull(planCollection);
    Objects.requireNonNull(activityCollection);
    Objects.requireNonNull(adaptationCollection);
    Objects.requireNonNull(simulationResultsCollection);
  }
}
