package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class Profile<Dynamics, Condition>
    implements Iterable<Pair<Window, Dynamics>>
{
  private final ResourceSolver<?, ?, Dynamics, Condition> solver;
  private final List<Pair<Window, Dynamics>> pieces;
  private Duration duration;

  private Profile(
      final ResourceSolver<?, ?, Dynamics, Condition> solver,
      final List<Pair<Window, Dynamics>> pieces,
      final Duration duration)
  {
    this.solver = solver;
    this.pieces = pieces;
    this.duration = duration;
  }

  public Profile(final ResourceSolver<?, ?, Dynamics, Condition> solver) {
    this(Objects.requireNonNull(solver), new ArrayList<>(), Duration.ZERO);
  }

  public Duration getDuration() {
    return this.duration;
  }

  public Profile<Dynamics, Condition> append(Dynamics dynamics) {
    this.pieces.add(Pair.of(Window.at(this.duration), dynamics));

    return this;
  }

  public Profile<Dynamics, Condition> extendBy(final Duration duration) {
    if (duration.isNegative()) throw new IllegalArgumentException("cannot extend by a negative duration");
    else if (duration.isZero()) return this;

    if (this.pieces.isEmpty()) throw new IllegalStateException("cannot extend an empty profile");

    final var lastSegment = this.pieces.get(this.pieces.size() - 1);

    this.duration = this.duration.plus(duration);
    this.pieces.set(
        this.pieces.size() - 1,
        Pair.of(
            Window.between(lastSegment.getLeft().start, this.duration),
            lastSegment.getRight()));

    return this;
  }

  public ResourceSolver<?, ?, Dynamics, Condition> getSolver() {
    return this.solver;
  }

  @Override
  public Iterator<Pair<Window, Dynamics>> iterator() {
    return Collections.unmodifiableList(this.pieces).iterator();
  }
}
