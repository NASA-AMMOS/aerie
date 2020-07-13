package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.DynamicActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.DynamicStateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.CumulableEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.CumulableStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.RegisterState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell.setDynamic;

public class SampleQuerier<T> implements MerlinAdaptation.Querier<T, SampleEvent> {
    // Create two DynamicCells to provide ReactionContext and StateContext to modeling code
    private final static DynamicCell<ReactionContext<?, Activity, SampleEvent>> reactionContext = DynamicCell.create();
    private final static DynamicCell<SampleQuerier<?>.StateQuerier> stateContext = DynamicCell.create();

    // Define a function to take a state name and provide questions that can be asked based on current context
    public static final Function<String, StateQuery<SerializedParameter>> query = (name) ->
        new DynamicStateQuery<>(() -> stateContext.get().getRegisterQuery(name));

    // Provide access to activity information based on the current context.
    public static final ActivityModelQuerier activityQuerier =
        new DynamicActivityModelQuerier(() -> stateContext.get().getActivityQuery());

    // Provide direct access to methods on the context stored in the dynamic cell.
    // e.g. instead of `reactionContext.get().spawn(act)`, just use `ctx.spawn(act)`.
    public static final ReactionContext<?, Activity, SampleEvent> ctx = new DynamicReactionContext<>(() -> reactionContext.get());

    // Maintain a map of Query objects for each state (by name)
    // This allows queries on states to be tracked and cached for convenience
    private final Map<String, Query<T, SampleEvent, RegisterState<Double>>> registers = new HashMap<>();

    // Model the durations of (and relationships between) activities.
    private final ActivityMapper activityMapper;
    private final Query<T, SampleEvent, ActivityModel> activityModel;

    public SampleQuerier(final ActivityMapper activityMapper, final SimulationTimeline<T, SampleEvent> timeline) {
        this.activityMapper = activityMapper;

        this.activityModel = timeline.register(
            new ActivityEffectEvaluator().filterContramap(SampleEvent::asActivity),
            new ActivityModelApplicator());

        // Register a Query object for each state
        for (final var entry : SampleMissionStates.factory.getCumulableStates().entrySet()) {
            final var name = entry.getKey();
            final var initialValue = entry.getValue();

            final var query = timeline.register(
                    new CumulableEffectEvaluator(name).filterContramap(SampleEvent::asIndependent),
                    new CumulableStateApplicator(initialValue));

            this.registers.put(name, query);
        }
    }

    @Override
    public void runActivity(final ReactionContext<T, Activity, SampleEvent> ctx, final String activityId, final Activity activity) {
        // Run the activity in the context of the given reaction context as well as the established state queries.
        // The activity can affect the simulation by emitting events against the reaction context,
        // and can query states to change its behavior based on simulation state.
        stateContext.setWithin(new StateQuerier(ctx::now), () ->
            reactionContext.setWithin(ctx, () -> {
                // Signal the beginning of the activity.
                ctx.emit(SampleEvent.activity(ActivityEvent.startActivity(activityId, this.activityMapper.serializeActivity(activity).get())));

                // Run the entirety of the activity.
                activity.modelEffects();

                // Signal the end of the activity, but only after all of its children have also completed.
                ctx.waitForChildren();
                ctx.emit(SampleEvent.activity(ActivityEvent.endActivity(activityId)));
            }));
    }

    @Override
    public Set<String> states() {
        return registers.keySet();
    }

    @Override
    public SerializedParameter getSerializedStateAt(final String name, final History<T, SampleEvent> history) {
        return this.getRegisterQueryAt(name, history).get();
    }

    @Override
    public List<ConstraintViolation> getConstraintViolationsAt(final History<T, SampleEvent> history) {
        return setDynamic(stateContext, new StateQuerier(() -> history), () -> {
            final var violations = new ArrayList<ConstraintViolation>();

            for (final var violableConstraint : SampleMissionStates.violableConstraints) {
                // Set the constraint's getWindows method within the context of the history and evaluate it
                final var violationWindows = violableConstraint.getWindows();
                if (violationWindows.isEmpty()) continue;

                violations.add(new ConstraintViolation(violationWindows, violableConstraint));
            }

            return violations;
        });
    }

    public StateQuery<SerializedParameter> getRegisterQueryAt(final String name, final History<T, SampleEvent> history) {
        return StateQuery.from(this.registers.get(name).getAt(history), SerializedParameter::of);
    }

    // An inner class to maintain a supplier for current history to pass to the SampleQuerier
    public final class StateQuerier {
        // Provides the most up-to-date event history at the time of each request
        private final Supplier<History<T, SampleEvent>> historySupplier;

        public StateQuerier(final Supplier<History<T, SampleEvent>> historySupplier) {
            this.historySupplier = historySupplier;
        }

        // Get a queryable object representing the named state.
        public StateQuery<SerializedParameter> getRegisterQuery(final String name) {
            return SampleQuerier.this.getRegisterQueryAt(name, this.historySupplier.get());
        }

        // Get a queryable object representing simulated activities.
        public ActivityModelQuerier getActivityQuery() {
            return SampleQuerier.this.activityModel.getAt(this.historySupplier.get());
        }
    }
}
