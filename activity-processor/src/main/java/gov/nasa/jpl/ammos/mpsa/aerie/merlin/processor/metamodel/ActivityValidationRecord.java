package gov.nasa.jpl.ammos.mpsa.aerie.merlin.processor.metamodel;

import java.util.Objects;

public final class ActivityValidationRecord {
  public final String methodName;
  public final String failureMessage;

  public ActivityValidationRecord(final String methodName, final String failureMessage) {
    this.methodName = Objects.requireNonNull(methodName);
    this.failureMessage = Objects.requireNonNull(failureMessage);
  }
}
