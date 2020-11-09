package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;

import java.util.List;

public interface ActivityInstance {
  SerializedActivity serialize();

  default List<String> getValidationFailures() {
    return List.of();
  }
}
