package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ConsumableState {
  private final String name;
  private final Function<String, StateQuery<Double>> model;
  private final Consumer<IndependentStateEvent> emitter;

  public ConsumableState(final String name, final Function<String, StateQuery<Double>> model, final Consumer<IndependentStateEvent> emitter) {
    this.name = name;
    this.model = model;
    this.emitter = emitter;
  }

  public void add(final double delta) {
    this.emitter.accept(IndependentStateEvent.add(this.name, delta));
  }

  public double get() {
    return this.model.apply(this.name).get();
  }

  public Constraint when(final Predicate<Double> condition) {
    return () -> this.model.apply(this.name).when(condition);
  }

  public Constraint whenGreaterThan(final double y) {
    return this.when((x) -> (x > y));
  }

  public Constraint whenLessThan(final double y) {
    return this.when((x) -> (x < y));
  }

  public Constraint whenLessThanOrEqualTo(final double y) {
    return this.when((x) -> (x <= y));
  }

  public Constraint whenGreaterThanOrEqualTo(final double y) {
    return this.when((x) -> (x >= y));
  }

  public Constraint whenEqualWithin(final double y, final double tolerance) {
    return this.when((x) -> (Math.abs(x - y) < tolerance));
  }
}
