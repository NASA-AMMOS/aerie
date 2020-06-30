package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SettableState {
  private final String name;
  private final Function<String, StateQuery<Double>> model;
  private final Consumer<IndependentStateEvent> emitter;

  public SettableState(final String name, final Function<String, StateQuery<Double>> model, final Consumer<IndependentStateEvent> emitter) {
    this.name = name;
    this.model = model;
    this.emitter = emitter;
  }

  public double get() {
    return this.model.apply(this.name).get();
  }

  public void set(double value) {
    this.emitter.accept(IndependentStateEvent.set(this.name, value));
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
