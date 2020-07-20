package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.MapEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.EventProjection;

import java.util.Map;

public final class DataEffectEvaluator extends EventProjection<Map<String, SettableEffect<Double, Double>>> {
  public DataEffectEvaluator() {
    super(new MapEffectTrait<>(
        new SettableEffectTrait<>(
            new SumEffectTrait(),
            (base, delta) -> base + delta,
            delta -> (delta == 0))));
  }

  @Override
  public final Map<String, SettableEffect<Double, Double>> addDataRate(final String binName, final double amount) {
    return Map.of(binName, SettableEffect.add(amount));
  }

  @Override
  public final Map<String, SettableEffect<Double, Double>> clearDataRate(final String binName) {
    return Map.of(binName, SettableEffect.setTo(0.0));
  }
}
