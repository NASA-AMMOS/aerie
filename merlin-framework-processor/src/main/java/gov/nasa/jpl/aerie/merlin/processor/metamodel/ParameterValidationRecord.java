package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import java.util.Objects;

public final class ParameterValidationRecord {
  public final String methodName;
  public final String failureMessage;

  public ParameterValidationRecord(final String methodName, final String failureMessage) {
    this.methodName = Objects.requireNonNull(methodName);
    this.failureMessage = Objects.requireNonNull(failureMessage);
  }
}
