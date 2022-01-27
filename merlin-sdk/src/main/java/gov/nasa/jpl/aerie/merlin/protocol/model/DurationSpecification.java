package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;
import java.util.function.Function;

public interface DurationSpecification {
  sealed interface DurationType {
    record Constant(Duration duration) implements DurationType {}
    record DirectlyControllable(String parameterName, Duration minValue, Duration maxValue) implements DurationType {}
    record ComputableFromParameters(Duration minValue, Duration maxValue, Function<Duration, Map<String, SerializedValue>> oracle) implements DurationType {}
    record ContextDependent(DurationBounds durationBounds) implements DurationType {}

    /**
     * Given two DurationTypes pertaining to two sibling activity instances, return the combined DurationType
     * representing the two activities together.
     *
     * TODO is this possible in general?
     * TODO is this operation associative? if not, is there a canonical evaluation order?
     */
    static DurationType combine(final DurationType first, final DurationType second) {
      if (orderIndex(first) > orderIndex(second)) {
        return combineHelper(second, first);
      } else {
        return combineHelper(first, second);
      }
    }

    static DurationType combineMultiple(final DurationType first, final DurationType... rest) {
      var acc = first;
      for (final var durationType : rest) {
        acc = combine(acc, durationType);
      }
      return acc;
    }

    static DurationType combineHelper(final DurationType first, final DurationType second) {
      if (first instanceof Constant constant1 && second instanceof Constant constant2) {
        return new Constant(constant1.duration().plus(constant2.duration()));
      } else if (first instanceof Constant constant && second instanceof DirectlyControllable directlyControllable) {
        return new ComputableFromParameters(
            constant.duration().plus(directlyControllable.minValue()),
            constant.duration().plus(directlyControllable.maxValue()),
            duration -> {throw new NotImplementedException();});
      } else if (first instanceof Constant constant && second instanceof ComputableFromParameters computableFromParameters) {
        return new ComputableFromParameters(
            constant.duration().plus(computableFromParameters.minValue()),
            constant.duration().plus(computableFromParameters.maxValue()),
            duration -> {throw new NotImplementedException();});
      } else if (first instanceof Constant constant && second instanceof ContextDependent contextDependent) {
        // TODO long overflow?
        return new ContextDependent(new DurationBounds(constant.duration().plus(contextDependent.durationBounds().min()),
                                                       constant.duration().plus(contextDependent.durationBounds().max())));
      } else if (first instanceof DirectlyControllable directlyControllable1 && second instanceof DirectlyControllable directlyControllable2) {
        return new ComputableFromParameters(
            directlyControllable1.minValue().plus(directlyControllable2.minValue()),
            directlyControllable1.maxValue().plus(directlyControllable2.maxValue()),
            duration -> {throw new NotImplementedException();});
      } else if (first instanceof DirectlyControllable directlyControllable && second instanceof ComputableFromParameters computableFromParameters) {
        return new ComputableFromParameters(
            directlyControllable.minValue().plus(computableFromParameters.minValue()),
            directlyControllable.maxValue().plus(computableFromParameters.maxValue()),
            duration -> {throw new NotImplementedException();});
      } else if (first instanceof DirectlyControllable directlyControllable && second instanceof ContextDependent contextDependent) {
        return new ContextDependent(new DurationBounds(
            directlyControllable.minValue().plus(contextDependent.durationBounds().min()),
            directlyControllable.maxValue().plus(contextDependent.durationBounds().max())));
      } else if (first instanceof ComputableFromParameters computableFromParameters1 && second instanceof ComputableFromParameters computableFromParameters2) {
        return new ComputableFromParameters(computableFromParameters1.minValue().plus(computableFromParameters2.minValue()),
                                            computableFromParameters1.maxValue().plus(computableFromParameters2.maxValue()),
                                            duration -> {throw new NotImplementedException();});
      } else if (first instanceof ComputableFromParameters computableFromParameters && second instanceof ContextDependent contextDependent) {
        return new ContextDependent(new DurationBounds(
            computableFromParameters.minValue().plus(contextDependent.durationBounds().min()),
            computableFromParameters.maxValue().plus(contextDependent.durationBounds().max())));
      } else if (first instanceof ContextDependent contextDependent1 && second instanceof ContextDependent contextDependent2) {
        return new ContextDependent(new DurationBounds(
            contextDependent1.durationBounds().min().plus(contextDependent2.durationBounds().min()),
            contextDependent2.durationBounds().max().plus(contextDependent2.durationBounds().max())));
      }
      throw new IllegalStateException("Illegal combination of DurationTypes: " + first + ", " + second);
    }

    /**
     * Impose an ordering on the types, just so the combineHelper function can assume that first < second
     */
    private static int orderIndex(final DurationType durationType) {
      if (durationType instanceof Constant) {
        return 1;
      } else if (durationType instanceof DirectlyControllable) {
        return 2;
      } else if (durationType instanceof ComputableFromParameters) {
        return 3;
      } else if (durationType instanceof ContextDependent) {
        return 4;
      } else {
        throw new Error("DurationType check not exhaustive");
      }
    }
  }

  record DurationBounds(Duration min, Duration max) {}

  DurationSpecification.DurationType getDurationType();

  DurationSpecification BROADEST = () -> new DurationType.ContextDependent(new DurationBounds(Duration.ZERO, Duration.MAX_VALUE));
}
