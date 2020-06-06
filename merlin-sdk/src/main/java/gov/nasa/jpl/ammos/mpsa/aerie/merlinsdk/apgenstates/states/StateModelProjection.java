package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.MutatingProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.traits.SettableEffect;

import java.util.Map;

public final class StateModelProjection
        extends MutatingProjection<StateModel, Map<String, SettableEffect<Double, Double>>>
{
    public StateModelProjection() {
        super(new StateEffectEvaluator());
    }

    @Override
    protected final StateModel fork(final StateModel model) {
        return new StateModel(model);
    }

    @Override
    protected final void apply(final StateModel model, final Map<String, SettableEffect<Double, Double>> effect) {

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
}