package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.RegisterStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.StateEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.RegisterState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BananaQuerier<T> implements MerlinAdaptation.Querier<T, BananaEvent> {
  private static final DynamicCell<Pair<ReactionContext<?, Activity, BananaEvent>, BananaQuerier<?>.InnerQuerier>> activeContext = DynamicCell.create();
  public static final Function<String, StateQuery<Double>> query = (name) -> new StateQuery<>() {
    @Override
    public Double get() {
      return activeContext.get().getRight().get(name);
    }

    @Override
    public List<Window> when(final Predicate<Double> condition) {
      return activeContext.get().getRight().when(name, condition);
    }
  };
  public static final ReactionContext<?, Activity, BananaEvent> ctx = new DynamicReactionContext<>(() -> activeContext.get().getLeft());


  private final Map<String, Query<T, BananaEvent, RegisterState>> registers = new HashMap<>();

  public BananaQuerier(final SimulationTimeline<T, BananaEvent> timeline) {
    for (final var entry : BananaStates.factory.getRegisteredStates().entrySet()) {
      final var name = entry.getKey();
      final var initialValue = entry.getValue();

      final var query = timeline.register(
          new StateEffectEvaluator(name).filterContramap(BananaEvent::asIndependent),
          new RegisterStateApplicator(initialValue));

      this.registers.put(name, query);
    }
  }

  @Override
  public void runActivity(final ReactionContext<T, Activity, BananaEvent> ctx, final Activity activity) {
    BananaQuerier.activeContext.setWithin(Pair.of(ctx, new InnerQuerier(ctx::now)), activity::modelEffects);
  }

  @Override
  public Set<String> states() {
    return this.registers.keySet();
  }

  public double getStateAt(final String name, final History<T, BananaEvent> history) {
    return this.registers.get(name).getAt(history).get();
  }

  @Override
  public SerializedParameter getSerializedStateAt(final String name, final History<T, BananaEvent> history) {
    return SerializedParameter.of(this.getStateAt(name, history));
  }

  public List<Window> whenStateUptoMatches(final String name, final History<T, BananaEvent> history, final Predicate<Double> condition) {
    return this.registers.get(name).getAt(history).when(condition);
  }

  @Override
  public List<ConstraintViolation> getConstraintViolationsAt(final History<T, BananaEvent> history) {
    final List<ConstraintViolation> violations = new ArrayList<>();

    for (final var violableConstraint : BananaStates.violableConstraints) {
      final var violationWindows = BananaQuerier.activeContext.setWithin(Pair.of(ctx, new InnerQuerier(() -> history)), violableConstraint::getWindows);
      if (!violationWindows.isEmpty()) {
        violations.add(new ConstraintViolation(violationWindows, violableConstraint));
      }
    }

    return violations;
  }

  public final class InnerQuerier {
    private final Supplier<History<T, BananaEvent>> currentHistory;

    private InnerQuerier(final Supplier<History<T, BananaEvent>> currentHistory) {
      this.currentHistory = currentHistory;
    }

    public double get(final String name) {
      return BananaQuerier.this.getStateAt(name, this.currentHistory.get());
    }

    public List<Window> when(final String name, final Predicate<Double> condition) {
      return BananaQuerier.this.whenStateUptoMatches(name, this.currentHistory.get(), condition);
    }
  }
}
