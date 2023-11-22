package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import static gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching.failure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Labelled.labelled;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class CellRefV2 {
  private CellRefV2() {}

  /**
   * Allocate a new resource with an explicitly given effect type and effect trait.
   */
  public static <D extends Dynamics<?, D>, E extends DynamicsEffect<D>> CellRef<Labelled<E>, Cell<D>> allocate(ErrorCatching<Expiring<D>> initialDynamics, EffectTrait<Labelled<E>> effectTrait) {
    return CellRef.allocate(new Cell<>(initialDynamics), new CellType<>() {
      @Override
      public EffectTrait<Labelled<E>> getEffectType() {
        return effectTrait;
      }

      @Override
      public Cell<D> duplicate(Cell<D> cell) {
        return new Cell<>(cell.initialDynamics, cell.dynamics, cell.elapsedTime);
      }

      @Override
      public void apply(Cell<D> cell, Labelled<E> effect) {
        cell.initialDynamics = effect.data().apply(cell.dynamics).match(
            ErrorCatching::success,
            error -> failure(new RuntimeException(
                "Applying '%s' failed.".formatted(effect.name()), error)));
        cell.dynamics = cell.initialDynamics;
        cell.elapsedTime = ZERO;
      }

      @Override
      public void step(Cell<D> cell, Duration duration) {
        // Avoid accumulated round-off error in imperfect stepping
        // by always stepping up from the initial dynamics
        cell.elapsedTime = cell.elapsedTime.plus(duration);
        cell.dynamics = ErrorCatchingMonad.map(cell.initialDynamics, d ->
            expiring(d.data().step(cell.elapsedTime), d.expiry().minus(cell.elapsedTime)));
      }
    });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> noncommutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> {
          throw new UnsupportedOperationException(
              "Concurrent effects are not supported on this resource.");
        });
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> commutingEffects() {
    return resolvingConcurrencyBy((left, right) -> x -> right.apply(left.apply(x)));
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> autoEffects() {
    return autoEffects(testing((CommutativityTestInput<D> input) -> input.leftResult.equals(input.rightResult)));
  }

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> autoEffects(
      Predicate<CommutativityTestInput<ErrorCatching<Expiring<D>>>> commutativityTest) {
    return resolvingConcurrencyBy((left, right) -> x -> {
      final var lrx = left.apply(right.apply(x));
      final var rlx = right.apply(left.apply(x));
      if (commutativityTest.test(new CommutativityTestInput<>(x, lrx, rlx))) {
        return lrx;
      } else {
        throw new UnsupportedOperationException(
            "Detected non-commuting concurrent effects!");
      }
    });
  }


  /**
   * Lift a commutativity test from data to dynamics,
   * correctly comparing expiry and error information in the process.
   */
  public static <D> Predicate<CommutativityTestInput<ErrorCatching<Expiring<D>>>> testing(Predicate<CommutativityTestInput<D>> test) {
    return input -> input.leftResult.match(
        leftExpiring -> input.rightResult.match(
            rightExpiring -> leftExpiring.expiry().equals(rightExpiring.expiry()) && test.test(new CommutativityTestInput<>(
                input.original.match(Expiring::data, $ -> leftExpiring.data()),
                leftExpiring.data(),
                rightExpiring.data())),
            rightError -> false),
        leftError -> input.rightResult.match(
            rightExpiring -> false,
            rightError -> Resources.equivalentExceptions(leftError, rightError)));
  }

  public record CommutativityTestInput<D>(D original, D leftResult, D rightResult) {}

  public static <D extends Dynamics<?, D>> EffectTrait<Labelled<DynamicsEffect<D>>> resolvingConcurrencyBy(BinaryOperator<DynamicsEffect<D>> combineConcurrent) {
    return new EffectTrait<>() {
      @Override
      public Labelled<DynamicsEffect<D>> empty() {
        return labelled("No-op", x -> x);
      }

      @Override
      public Labelled<DynamicsEffect<D>> sequentially(final Labelled<DynamicsEffect<D>> prefix, final Labelled<DynamicsEffect<D>> suffix) {
        return new Labelled<>(
            x -> suffix.data().apply(prefix.data().apply(x)),
            "(%s) then (%s)".formatted(prefix.name(), suffix.name()));
      }

      @Override
      public Labelled<DynamicsEffect<D>> concurrently(final Labelled<DynamicsEffect<D>> left, final Labelled<DynamicsEffect<D>> right) {
        try {
          final DynamicsEffect<D> combined = combineConcurrent.apply(left.data(), right.data());
          return new Labelled<>(
              x -> {
                try {
                  return combined.apply(x);
                } catch (Exception e) {
                  return failure(e);
                }
              },
              "(%s) and (%s)".formatted(left.name(), right.name()));
        } catch (Throwable e) {
          return new Labelled<>(
              $ -> failure(e),
              "Failed to combine concurrent effects: (%s) and (%s)".formatted(left.name(), right.name()));
        }
      }
    };
  }

  public static class Cell<D> {
    public ErrorCatching<Expiring<D>> initialDynamics;
    public ErrorCatching<Expiring<D>> dynamics;
    public Duration elapsedTime;

    public Cell(ErrorCatching<Expiring<D>> dynamics) {
      this(dynamics, dynamics, ZERO);
    }

    public Cell(ErrorCatching<Expiring<D>> initialDynamics, ErrorCatching<Expiring<D>> dynamics, Duration elapsedTime) {
      this.initialDynamics = initialDynamics;
      this.dynamics = dynamics;
      this.elapsedTime = elapsedTime;
    }
  }
}
