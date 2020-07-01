package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.RegisterState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.RegisterStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.StateEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SampleQuerier<T> implements MerlinAdaptation.Querier<T, SampleEvent> {

    // Create two DynamicCells to provide ReactionContext and StateContext to modeling code
    private final static DynamicCell<ReactionContext<?, Activity, SampleEvent>> reactionContext = DynamicCell.create();
    private final static DynamicCell<SampleQuerier<?>.StateQuerier> stateContext = DynamicCell.create();

    // Define a function to take a state name and provide questions that can be asked
    // based on current context
    public static final Function<String, StateQuery<Double>> query = (name) -> new StateQuery<>() {
        @Override
        public Double get() {
            return stateContext.get().getStateValue(name);
        }

        @Override
        public List<Window> when(final Predicate<Double> condition) {
            return stateContext.get().when(name, condition);
        }
    };

    // Provide direct access to methods on the context stored in the dynamic cell.
    // e.g. instead of `reactionContext.get().spawn(act)`, just use `ctx.spawn(act)`.
    public static final ReactionContext<?, Activity, SampleEvent> ctx = new DynamicReactionContext<>(() -> reactionContext.get());

    // Maintain a map of Query objects for each state (by name)
    // This allows queries on states to be tracked and cached for convenience
    private final Map<String, Query<T, SampleEvent, RegisterState>> registers = new HashMap<>();

    public SampleQuerier(final SimulationTimeline<T, SampleEvent> timeline, final IndependentStateFactory stateFactory) {
        // Register a Query object for each state
        for (final var entry : stateFactory.getRegisteredStates().entrySet()) {
            final var name = entry.getKey();
            final var initialValue = entry.getValue();

            final var query = timeline.register(
                    new StateEffectEvaluator(name).filterContramap(SampleEvent::asIndependent),
                    new RegisterStateApplicator(initialValue));

            this.registers.put(name, query);
        }
    }

    @Override
    public void runActivity(final ReactionContext<T, Activity, SampleEvent> ctx, final Activity activity) {
        // Set the activity within the current context to provide the ability build on the current history
        reactionContext.setWithin(ctx, () ->
                stateContext.setWithin(new StateQuerier(ctx::now), () ->
                        activity.modelEffects()));
    }

    @Override
    public Set<String> states() {
        return registers.keySet();
    }

    // Determine a state value from a History
    public Double getStateValue(final String name, final History<T, SampleEvent> history) {
        return this.registers.get(name).getAt(history).get();
    }

    // Determine the windows when a state meets a condition throughout a History
    public List<Window> whenStateMeetsCondition(final String name, final Predicate<Double> condition, final History<T, SampleEvent> history) {
        // Use the registered Query object for convenience
        return this.registers.get(name).getAt(history).when(condition);
    }

    @Override
    public SerializedParameter getSerializedStateAt(final String name, final History<T, SampleEvent> history) {
        return SerializedParameter.of(this.getStateValue(name, history));
    }

    @Override
    public List<ConstraintViolation> getConstraintViolationsAt(final History<T, SampleEvent> history) {
        final List<ConstraintViolation> violations = new ArrayList<>();

        final var stateQuerier = new StateQuerier(() -> history);
        for (final var violableConstraint : SampleMissionStates.violableConstraints) {
            // Set the constraint's getWindows method within the context of the history and evaluate it
            final var violationWindows = stateContext.setWithin(stateQuerier, violableConstraint::getWindows);
            if (!violationWindows.isEmpty()) {
                violations.add(new ConstraintViolation(violationWindows, violableConstraint));
            }
        }

        return violations;
    }

    // An inner class to maintain a supplier for current history to pass to the SampleQuerier
    public final class StateQuerier {
        // Provides the most up-to-date event history at the time of each request
        private final Supplier<History<T, SampleEvent>> historySupplier;

        public StateQuerier(final Supplier<History<T, SampleEvent>> historySupplier) {
            this.historySupplier = historySupplier;
        }

        // Get the value of the named state at the current point in time.
        public Double getStateValue(final String name) {
            return SampleQuerier.this.getStateValue(name, historySupplier.get());
        }

        // Determine when a state meets a condition between simulation start and the current point in time.
        public List<Window> when(final String name, final Predicate<Double> condition) {
            return SampleQuerier.this.whenStateMeetsCondition(name, condition, historySupplier.get());
        }
    }
}
