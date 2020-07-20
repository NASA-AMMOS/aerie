package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public final class DataModelApplicator
    implements Applicator<Map<String, SettableEffect<Double, Double>>, DataModel>
{
  @Override
  public DataModel initial() {
    final var initial = new DataModel();
    initial.getBin("bin A").addRate(1.0);
    initial.getBin("bin B").setVolume(5.0);
    return initial;
  }

  @Override
  public DataModel duplicate(final DataModel model) {
    return new DataModel(model);
  }

  @Override
  public void step(final DataModel dataModel, final Duration duration) {
    dataModel.step(duration);
  }

  @Override
  public void apply(final DataModel model, final Map<String, SettableEffect<Double, Double>> effect) {
    for (final var entry : effect.entrySet()) {
      final var name = entry.getKey();
      final var change = entry.getValue();

      final var bin = model.getBin(name);

      change.visit(new SettableEffect.VoidVisitor<>() {
        @Override
        public void setTo(final Double value) {
          bin.setVolume(value);
        }

        @Override
        public void add(final Double delta) {
          bin.addRate(delta);
        }

        @Override
        public void conflict() {
          System.err.printf("Conflict! on bin `%s`\n", name);
          bin.setRate(0.0);
        }
      });
    }
  }
}
