package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface DurationSpecification {
  enum DurationType {
    Constant,
    ComputableFromParameters,
    ContextDependent,
    DirectlyControllable;

    public static DurationType combine(DurationType first, DurationType second) {
      if (first == Constant) {
        return second;
      }
      if (second == Constant) {
        return first;
      }
      if (first == ContextDependent || second == ContextDependent) {
        return ContextDependent;
      }
      if (first == ComputableFromParameters || second == ComputableFromParameters) {
        return ComputableFromParameters;
      }
    }
  }

  record DurationBounds(Duration min, Duration max) {}

  DurationSpecification.DurationType getDurationType();

  DurationSpecification.DurationBounds getDurationBounds();
}
