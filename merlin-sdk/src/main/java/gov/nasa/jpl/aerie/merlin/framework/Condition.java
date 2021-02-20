package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.Optional;

public interface Condition {
  Optional<Duration> nextSatisfied(Window scope, boolean positive);

  default Condition and(final Condition other) {
    return and(this, other);
  }

  default Condition or(final Condition other) {
    return or(this, other);
  }

  default Condition not() {
    return not(this);
  }


  Condition TRUE = (scope, positive) -> Optional.of(scope.start);
  Condition FALSE = (scope, positive) -> Optional.empty();

  static Condition or(final Condition left, final Condition right) {
    return (scope, positive) -> {
      if (!positive) return and(not(left), not(right)).nextSatisfied(scope, positive);

      final var left$ = left.nextSatisfied(scope, positive);
      final var right$ = right.nextSatisfied(scope, positive);

      if (left$.isEmpty()) return right$;
      if (right$.isEmpty()) return left$;
      return Optional.of(Duration.min(left$.get(), right$.get()));
    };
  }

  static Condition and(final Condition left, final Condition right) {
    return (scope, positive) -> {
      if (!positive) return or(not(left), not(right)).nextSatisfied(scope, positive);

      Optional<Duration> left$, right$;

      left$ = left.nextSatisfied(scope, positive);
      if (left$.isEmpty()) return Optional.empty();

      while (true) {
        scope = Window.greatestLowerBound(scope, Window.between(left$.get(), Duration.MAX_VALUE));
        if (scope.isEmpty()) break;

        right$ = right.nextSatisfied(scope, positive);
        if (right$.isEmpty()) break;
        if (right$.get().isEqualTo(left$.get())) return left$;

        scope = Window.greatestLowerBound(scope, Window.between(right$.get(), Duration.MAX_VALUE));
        if (scope.isEmpty()) break;

        left$ = left.nextSatisfied(scope, positive);
        if (left$.isEmpty()) break;
        if (left$.get().isEqualTo(right$.get())) return right$;
      }

      return Optional.empty();
    };
  }

  static Condition not(final Condition base) {
    return (scope, positive) ->
        base.nextSatisfied(scope, !positive);
  }
}
