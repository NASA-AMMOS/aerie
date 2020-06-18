package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public final class StateModelApplicator implements Applicator<Map<String, SettableEffect<Double, Double>>, StateModel> {
    @Override
    public StateModel initial() {
        return new StateModel();
    }

    @Override
    public final StateModel duplicate(final StateModel model) {
        return new StateModel(model);
    }

    @Override
    public final void apply(final StateModel model, final Map<String, SettableEffect<Double, Double>> effect) {
        for (final var entry : effect.entrySet()) {
            final var name = entry.getKey();
            final var change = entry.getValue();

            final var state = model.getState(name);

            change.visit(new SettableEffect.VoidVisitor<>() {
                @Override
                public void setTo(final Double value) {
                    state.set(value);
                }

                @Override
                public void add(final Double delta) {
                    state.add(delta);
                }

                @Override
                public void conflict() {
                    System.err.printf("Conflict! on state `%s`\n", name);
                    state.set(0.0);
                    //todo: record states explicitly in conflict
                }
            });
        }
    }

    @Override
    public void step(final StateModel stateModel, final Duration duration) {

    }
}
