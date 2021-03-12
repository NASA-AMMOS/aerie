package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.Optional;

public interface Condition {
  Optional<Duration> nextSatisfied(boolean positive, Window scope);

  default Condition and(final Condition other) {
    return and(this, other);
  }

  default Condition or(final Condition other) {
    return or(this, other);
  }

  default Condition not() {
    return not(this);
  }


  Condition TRUE = (positive, scope) -> Optional.of(scope.start);
  Condition FALSE = (positive, scope) -> Optional.empty();

  static Condition or(final Condition left, final Condition right) {
    return (positive, scope) -> {
      if (scope.isEmpty()) return Optional.empty();
      if (!positive) return and(not(left), not(right)).nextSatisfied(positive, scope);

      final var left$ = left.nextSatisfied(positive, scope);
      final var right$ = right.nextSatisfied(positive, scope);

      if (left$.isEmpty()) return right$;
      if (right$.isEmpty()) return left$;
      return Optional.of(Duration.min(left$.get(), right$.get()));
    };
  }

  static Condition and(final Condition left, final Condition right) {
    return (positive, scope) -> {
      if (scope.isEmpty()) return Optional.empty();
      if (!positive) return or(not(left), not(right)).nextSatisfied(positive, scope);

      Optional<Duration> left$, right$;

      left$ = left.nextSatisfied(positive, scope);
      if (left$.isEmpty()) return Optional.empty();

      while (true) {
        scope = Window.between(left$.get(), scope.end);
        if (scope.isEmpty()) break;

        right$ = right.nextSatisfied(positive, scope);
        if (right$.isEmpty()) break;
        if (right$.get().isEqualTo(left$.get())) return left$;

        scope = Window.between(right$.get(), scope.end);
        if (scope.isEmpty()) break;

        left$ = left.nextSatisfied(positive, scope);
        if (left$.isEmpty()) break;
        if (left$.get().isEqualTo(right$.get())) return right$;
      }

      return Optional.empty();
    };
  }

  static Condition not(final Condition base) {
    return (positive, scope) ->
        base.nextSatisfied(!positive, scope);
  }
}
