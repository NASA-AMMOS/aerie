package gov.nasa.jpl.aerie.merlin.worker;

import gov.nasa.jpl.aerie.merlin.server.config.Store;

import java.nio.file.Path;
import java.util.Objects;

public record WorkerAppConfiguration(
    Path merlinFileStore,
    Store store
) {
  public WorkerAppConfiguration {
    Objects.requireNonNull(merlinFileStore);
    Objects.requireNonNull(store);
  }
}
