package gov.nasa.jpl.aerie.merlin.server.config;

import java.nio.file.Path;
import java.util.Objects;

public record AppConfiguration (
    int httpPort,
    boolean enableJavalinDevLogging,
    Path merlinFileStore,
    Store store,
    boolean useNewConstraintPipeline
) {
  public AppConfiguration {
    Objects.requireNonNull(merlinFileStore);
    Objects.requireNonNull(store);
  }
}
