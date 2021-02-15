package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import gov.nasa.jpl.aerie.time.Windows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public final class WindowAccumulator<D, C> {
  private final ResourceSolver<?, ?, D, C> solver;
  private final C condition;
  private final Iterator<Pair<Window, D>> iter;

  private WindowAccumulator(
      final ResourceSolver<?, ?, D, C> solver,
      final C condition,
      final Iterator<Pair<Window, D>> iter)
  {
    this.solver = Objects.requireNonNull(solver);
    this.condition = Objects.requireNonNull(condition);
    this.iter = Objects.requireNonNull(iter);
  }

  public static <R, C, D, $Schema>
  Windows solve(
      final Window planWindow,
      final ResourceSolver<$Schema, R, D, C> solver,
      final C condition,
      final Iterable<Pair<Window, D>> profile)
  {
    final var iter = profile.iterator();
    final var foo = new WindowAccumulator<>(solver, condition, iter);
    return foo.satisfying(iter.next(), planWindow);
  }

  private Windows satisfying(Pair<Window, D> currentDynamics, Window remainingWindow) {
    // Look for the soonest satisfying time across all dynamics.
    Optional<Duration> satisfyingTime;
    do {
      satisfyingTime = solver.firstSatisfied(
          currentDynamics.getRight(),
          condition,
          Window.between(remainingWindow.start, Duration.min(currentDynamics.getLeft().end, remainingWindow.end)));

      if (satisfyingTime.isPresent() || !iter.hasNext()) {
        break;
      }

      remainingWindow = Window.between(currentDynamics.getLeft().end, remainingWindow.end);
      currentDynamics = iter.next();
    } while (true);

    // If there's never a satisfying time, we have no windows.
    if (satisfyingTime.isEmpty()) {
      return new Windows();
    }

    // We found a satisfying time -- look for its matching partner.
    return dissatisfying(
        satisfyingTime.get(),
        currentDynamics,
        Window.subtract(
            remainingWindow,
            Window.between(Duration.MIN_VALUE, satisfyingTime.get())));
  }

  private Windows
  dissatisfying(final Duration satisfyingTime, Pair<Window, D> currentDynamics, Window remainingWindow)
  {
    // Look for the soonest dissatisfying time across all dynamics.
    Optional<Duration> dissatisfyingTime;
    do {
      dissatisfyingTime = solver.firstDissatisfied(
          currentDynamics.getRight(),
          condition,
          Window.between(remainingWindow.start, Duration.min(currentDynamics.getLeft().end, remainingWindow.end)));

      if (dissatisfyingTime.isPresent() || !iter.hasNext()) {
        break;
      }

      remainingWindow = Window.between(currentDynamics.getLeft().end, remainingWindow.end);
      currentDynamics = iter.next();
    } while (true);

    // If there's never a dissatisfying time, the window extends as far as possible.
    if (dissatisfyingTime.isEmpty()) {
      return new Windows(Window.between(satisfyingTime, remainingWindow.end));
    }

    // We found a dissatisfying time -- look for more windows, and include this one with the rest.
    final var windows = satisfying(
        currentDynamics,
        Window.subtract(
            remainingWindow,
            Window.between(Duration.MIN_VALUE, dissatisfyingTime.get())));

    windows.add(satisfyingTime, dissatisfyingTime.get());

    return windows;
  }
}
