package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProfileBuilder<$Schema, Resource, Dynamics> {
  public final ResourceSolver<$Schema, Resource, Dynamics> solver;
  public final Resource resource;
  public final List<Pair<Duration, Dynamics>> pieces;
  public final Set<Query<?, ?, ?>> lastDependencies;

  public ProfileBuilder(
      final ResourceSolver<$Schema, Resource, Dynamics> solver,
      final Resource resource)
  {
    this.solver = solver;
    this.resource = resource;
    this.pieces = new ArrayList<>();
    this.lastDependencies = new HashSet<>();
  }

  public <$Timeline extends $Schema>
  void updateAt(final Adaptation<$Schema> adaptation, final History<$Timeline> history) {
    this.lastDependencies.clear();

    final var dynamics = this.solver.getDynamics(this.resource, new Querier<$Timeline>() {
      @Override
      public <State> State getState(final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Timeline, ?, State> token) {
        final var query = adaptation
            .getQuery(token.specialize())
            .orElseThrow(() -> new IllegalArgumentException("forged token"));

        ProfileBuilder.this.lastDependencies.add(query);
        return history.ask(query);
      }
    });

    this.pieces.add(Pair.of(Duration.ZERO, dynamics));
  }

  public void extendBy(final Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("cannot extend by a negative duration");
    } else if (duration.isZero()) {
      return;
    }

    if (this.pieces.isEmpty()) throw new IllegalStateException("cannot extend an empty profile");

    final var lastSegment = this.pieces.get(this.pieces.size() - 1);
    final var extent = lastSegment.getLeft();
    final var dynamics = lastSegment.getRight();

    this.pieces.set(
        this.pieces.size() - 1,
        Pair.of(extent.plus(duration), dynamics));
  }

  public List<Pair<Duration, Dynamics>> build() {
    return Collections.unmodifiableList(this.pieces);
  }
}
