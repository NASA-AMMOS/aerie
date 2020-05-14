package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.MutatingProjection;

import java.util.Map;

public final class DataModelProjection
    extends MutatingProjection<DataModel, Map<String, SettableEffect<Double, Double>>>
{
  public DataModelProjection() {
    super(new DataEffectEvaluator());
  }

  @Override
  protected final DataModel fork(final DataModel model) {
    return new DataModel(model);
  }

  @Override
  protected final void apply(final DataModel model, final Map<String, SettableEffect<Double, Double>> effect) {
    for (final var entry : effect.entrySet()) {
      final var name = entry.getKey();
      final var change = entry.getValue();

      final var bin = model.getDataBin(name);

      change.visit(new SettableEffect.VoidVisitor<>() {
        @Override
        public void setTo(final Double value) {
          bin.setVolume(value);
        }

        @Override
        public void add(final Double delta) {
          bin.addVolume(delta);
        }

        @Override
        public void conflict() {
          System.err.printf("Conflict! on bin `%s`\n", name);
          bin.setVolume(0.0);
        }
      });
    }
  }
}
