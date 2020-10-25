package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.paired;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Solver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public final class PairedSolver<L, LDyn, LCond, R, RDyn, RCond>
    implements Solver<Pair<L, R>, Pair<LDyn, RDyn>, Pair<LCond, RCond>>
{
  private final Solver<L, LDyn, LCond> leftSolver;
  private final Solver<R, RDyn, RCond> rightSolver;

  public PairedSolver(final Solver<L, LDyn, LCond> leftSolver, final Solver<R, RDyn, RCond> rightSolver) {
    this.leftSolver = Objects.requireNonNull(leftSolver);
    this.rightSolver = Objects.requireNonNull(rightSolver);
  }

  @Override
  public Pair<L, R> valueAt(final Pair<LDyn, RDyn> dynamics, final Duration elapsedTime) {
    return Pair.of(
        this.leftSolver.valueAt(dynamics.getLeft(), elapsedTime),
        this.rightSolver.valueAt(dynamics.getRight(), elapsedTime));
  }

  @Override
  public Windows whenSatisfied(final Pair<LDyn, RDyn> dynamics, final Pair<LCond, RCond> condition) {
    final var leftWindows = this.leftSolver.whenSatisfied(dynamics.getLeft(), condition.getLeft());
    final var rightWindows = this.rightSolver.whenSatisfied(dynamics.getRight(), condition.getRight());

    final var windows = new Windows(leftWindows);
    windows.intersectWith(rightWindows);
    return windows;
  }
}
