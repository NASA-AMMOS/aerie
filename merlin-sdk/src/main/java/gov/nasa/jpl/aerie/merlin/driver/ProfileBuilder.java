package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProfileBuilder<$Schema, Resource, Dynamics, Condition> {
  public final ResourceSolver<$Schema, Resource, Dynamics, Condition> solver;
  public final Resource resource;
  public final List<Pair<Window, Dynamics>> pieces;
  public final Set<Query<? super $Schema, ?, ?>> lastDependencies;

  public ProfileBuilder(
      final ResourceSolver<$Schema, Resource, Dynamics, Condition> solver,
      final Resource resource)
  {
    this.solver = solver;
    this.resource = resource;
    this.pieces = new ArrayList<>();
    this.lastDependencies = new HashSet<>();
  }

  public void updateAt(final History<? extends $Schema> history) {
    final var start =
        (this.pieces.isEmpty())
            ? Duration.ZERO
            : this.pieces.get(this.pieces.size() - 1).getLeft().end;

    this.lastDependencies.clear();

    final var dynamics = this.solver.getDynamics(this.resource, new Checkpoint<>() {
      @Override
      public <Event, Model> Model ask(final Query<? super $Schema, Event, Model> query) {
        ProfileBuilder.this.lastDependencies.add(query);
        return history.ask(query);
      }
    });

    this.pieces.add(Pair.of(Window.at(start), dynamics));
  }

  public void extendBy(final Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("cannot extend by a negative duration");
    } else if (duration.isZero()) return;

    if (this.pieces.isEmpty()) throw new IllegalStateException("cannot extend an empty profile");

    final var lastSegment = this.pieces.get(this.pieces.size() - 1);
    final var lastWindow = lastSegment.getLeft();
    final var dynamics = lastSegment.getRight();

    this.pieces.set(
        this.pieces.size() - 1,
        Pair.of(
            Window.between(lastWindow.start, lastWindow.end.plus(duration)),
            dynamics));
  }

  public List<Pair<Window, Dynamics>> build() {
    return Collections.unmodifiableList(this.pieces);
  }
}
