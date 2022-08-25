package gov.nasa.jpl.aerie.merlin.server.config;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record AppConfiguration (
    int httpPort,
    boolean enableJavalinDevLogging,
    Path merlinFileStore,
    Store store,
    Instant untruePlanStart
) {
  public AppConfiguration {
    Objects.requireNonNull(merlinFileStore);
    Objects.requireNonNull(store);
    Objects.requireNonNull(untruePlanStart);
  }
}
