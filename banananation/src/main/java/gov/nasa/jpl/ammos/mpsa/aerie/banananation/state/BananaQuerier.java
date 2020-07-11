package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.DynamicStateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.CumulableEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.CumulableStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.SettableEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.SettableStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model.RegisterState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell.setDynamic;

public final class BananaQuerier<T> implements MerlinAdaptation.Querier<T, BananaEvent> {
  private static final DynamicCell<ReactionContext<?, Activity, BananaEvent>> reactionContext = DynamicCell.create();
  private static final DynamicCell<BananaQuerier<?>.InnerQuerier> queryContext = DynamicCell.create();

  public static final ReactionContext<?, Activity, BananaEvent> ctx = new DynamicReactionContext<>(reactionContext::get);
  public static final Function<String, StateQuery<SerializedParameter>> query = (name) -> new DynamicStateQuery<>(() -> queryContext.get().getRegisterQuery(name));


  private final Set<String> stateNames = new HashSet<>();
  private final Map<String, Query<T, BananaEvent, RegisterState<SerializedParameter>>> settables = new HashMap<>();
  private final Map<String, Query<T, BananaEvent, RegisterState<Double>>> cumulables = new HashMap<>();

  public BananaQuerier(final SimulationTimeline<T, BananaEvent> timeline) {
    for (final var entry : BananaStates.factory.getSettableStates().entrySet()) {
      final var name = entry.getKey();
      final var initialValue = entry.getValue();

      if (this.stateNames.contains(name)) throw new RuntimeException("State \"" + name + "\" already defined");
      this.stateNames.add(name);

      this.settables.put(name, timeline.register(
        new SettableEffectEvaluator(name).filterContramap(BananaEvent::asIndependent),
        new SettableStateApplicator(initialValue)));
    }

    for (final var entry : BananaStates.factory.getCumulableStates().entrySet()) {
      final var name = entry.getKey();
      final var initialValue = entry.getValue();

      if (this.stateNames.contains(name)) throw new RuntimeException("State \"" + name + "\" already defined");
      this.stateNames.add(name);

      this.cumulables.put(name, timeline.register(
        new CumulableEffectEvaluator(name).filterContramap(BananaEvent::asIndependent),
        new CumulableStateApplicator(initialValue)));
    }
  }

  @Override
  public void runActivity(final ReactionContext<T, Activity, BananaEvent> ctx, final String activityId, final Activity activity) {
    BananaQuerier.reactionContext.setWithin(ctx, () ->
        BananaQuerier.queryContext.setWithin(new InnerQuerier(ctx::now), () ->
            activity.modelEffects()));
  }

  @Override
  public Set<String> states() {
    return Collections.unmodifiableSet(this.stateNames);
  }

  @Override
  public SerializedParameter getSerializedStateAt(final String name, final History<T, BananaEvent> history) {
    return this.getRegisterQueryAt(name, history).get();
  }

  public StateQuery<SerializedParameter> getRegisterQueryAt(final String name, final History<T, BananaEvent> history) {
    if (this.settables.containsKey(name)) return this.settables.get(name).getAt(history);
    else if (this.cumulables.containsKey(name)) return StateQuery.from(this.cumulables.get(name).getAt(history), SerializedParameter::of);
    else throw new RuntimeException("State \"" + name + "\" is not defined");
  }

  @Override
  public List<ConstraintViolation> getConstraintViolationsAt(final History<T, BananaEvent> history) {
    final var violations = new ArrayList<ConstraintViolation>();

    final var innerQuerier = new InnerQuerier(() -> history);
    for (final var violableConstraint : BananaStates.violableConstraints) {
      final var violationWindows = BananaQuerier.queryContext.setWithin(innerQuerier, violableConstraint::getWindows);
      if (!violationWindows.isEmpty()) {
        violations.add(new ConstraintViolation(violationWindows, violableConstraint));
      }
    }

    return violations;
  }

  private final class InnerQuerier {
    private final Supplier<History<T, BananaEvent>> currentHistory;

    private InnerQuerier(final Supplier<History<T, BananaEvent>> currentHistory) {
      this.currentHistory = currentHistory;
    }

    public StateQuery<SerializedParameter> getRegisterQuery(final String name) {
      return BananaQuerier.this.getRegisterQueryAt(name, this.currentHistory.get());
    }
  }
}
