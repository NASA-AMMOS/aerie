package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.CollectingEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.delimited;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics.persistent;

public final class DurativeRealModel implements Model<Collection<DelimitedDynamics<RealDynamics>>, DurativeRealModel> {
  private final PriorityQueue<Pair<Window, RealDynamics>> activeEffects;
  private Duration elapsedTime;

  public DurativeRealModel() {
    this.activeEffects = new PriorityQueue<>(Comparator.comparing(x -> x.getLeft().end));
    this.elapsedTime = Duration.ZERO;
  }

  private DurativeRealModel(final DurativeRealModel other) {
    this.activeEffects = new PriorityQueue<>(other.activeEffects);
    this.elapsedTime = other.elapsedTime;
  }

  @Override
  public DurativeRealModel duplicate() {
    return new DurativeRealModel(this);
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

  public static final RealResource<DurativeRealModel> value = (model) -> {
    var acc = persistent(RealDynamics.constant(0.0));

    for (final var entry : model.activeEffects) {
      final var x = delimited(
          entry.getLeft().end.minus(model.elapsedTime),
          entry.getRight());
      acc = acc.parWith(x, RealDynamics::plus);
    }

    return acc;
  };
}
