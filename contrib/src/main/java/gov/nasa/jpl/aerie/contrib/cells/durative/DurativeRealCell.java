package gov.nasa.jpl.aerie.contrib.cells.durative;

import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

import static gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics.delimited;
import static gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics.persistent;

public final class DurativeRealCell implements Cell<Collection<DelimitedDynamics<RealDynamics>>, DurativeRealCell> {
  private final PriorityQueue<Pair<Window, RealDynamics>> activeEffects;
  private Duration elapsedTime;

  public DurativeRealCell() {
    this.activeEffects = new PriorityQueue<>(Comparator.comparing(x -> x.getLeft().end));
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
  public EffectTrait<Collection<DelimitedDynamics<RealDynamics>>> effectTrait() {
    return new CollectingEffectTrait<>();
  }

  @Override
  public void react(final Collection<DelimitedDynamics<RealDynamics>> effects) {
    for (final var dynamics : effects) {
      this.activeEffects.add(Pair.of(
          Window.between(this.elapsedTime, dynamics.getEndTime().plus(this.elapsedTime)),
          dynamics.getDynamics()));
    }
  }

  @Override
  public void step(final Duration duration) {
    this.elapsedTime = this.elapsedTime.plus(duration);

    final var iter = this.activeEffects.iterator();
    while (iter.hasNext()) {
      final var entry = iter.next();
      if (this.elapsedTime.shorterThan(entry.getLeft().end)) break;
      iter.remove();
    }

    if (this.activeEffects.isEmpty()) {
      this.elapsedTime = Duration.ZERO;
    }
  }

  public DelimitedDynamics<RealDynamics> getValue() {
    var acc = persistent(RealDynamics.constant(0.0));

    for (final var entry : this.activeEffects) {
      final var x = delimited(
          entry.getLeft().end.minus(this.elapsedTime),
          entry.getRight());
      acc = acc.parWith(x, RealDynamics::plus);
    }

    return acc;
  }
}
