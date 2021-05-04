package gov.nasa.jpl.aerie.contrib.cells.durative;

import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

import static gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics.delimited;

public final class DurativeRealCell implements Cell<Collection<Pair<Duration, RealDynamics>>, DurativeRealCell> {
  private final PriorityQueue<Pair<Duration, RealDynamics>> activeEffects;
  private Duration elapsedTime;

  public DurativeRealCell() {
    this.activeEffects = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));
    this.elapsedTime = Duration.ZERO;
  }

  private DurativeRealCell(final DurativeRealCell other) {
    this.activeEffects = new PriorityQueue<>(other.activeEffects);
    this.elapsedTime = other.elapsedTime;
  }

  @Override
  public DurativeRealCell duplicate() {
    return new DurativeRealCell(this);
  }

  @Override
  public EffectTrait<Collection<Pair<Duration, RealDynamics>>> effectTrait() {
    return new CollectingEffectTrait<>();
  }

  @Override
  public void react(final Collection<Pair<Duration, RealDynamics>> effects) {
    for (final var effect : effects) {
      this.activeEffects.add(Pair.of(
          this.elapsedTime.plus(effect.getLeft()),
          effect.getRight()));
    }
  }

  @Override
  public void step(final Duration duration) {
    this.elapsedTime = this.elapsedTime.plus(duration);

    final var iter = this.activeEffects.iterator();
    while (iter.hasNext()) {
      final var entry = iter.next();
      if (this.elapsedTime.shorterThan(entry.getLeft())) break;
      iter.remove();
    }

    if (this.activeEffects.isEmpty()) {
      this.elapsedTime = Duration.ZERO;
    }
  }

  public DelimitedDynamics<RealDynamics> getValue() {
    var duration = Duration.MAX_VALUE;
    var dynamics = RealDynamics.constant(0.0);

    for (final var entry : this.activeEffects) {
      duration = entry.getLeft().minus(this.elapsedTime);
      dynamics = dynamics.plus(entry.getRight());
    }

    return delimited(duration, dynamics);
  }
}
