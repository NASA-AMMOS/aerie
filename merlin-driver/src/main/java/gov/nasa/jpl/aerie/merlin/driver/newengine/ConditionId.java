package gov.nasa.jpl.aerie.merlin.driver.newengine;

import java.util.UUID;

/** A typed wrapper for condition IDs. */
/*package-local*/ record ConditionId(String id) {
  public static ConditionId generate() {
    return new ConditionId(UUID.randomUUID().toString());
  }
}
