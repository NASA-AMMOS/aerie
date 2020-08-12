package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class DoubleState implements MetricState<Double, Double> {
  private final String name;
  private final Supplier<Double> getter;
  private final Function<Predicate<Double>, Windows> checker;
  private final Consumer<Double> emitter;

  public DoubleState(
      final String name,
      final Supplier<Double> getter,
      final Function<Predicate<Double>, Windows> checker,
      final Consumer<Double> emitter
  ) {
    this.name = name;
    this.getter = getter;
    this.checker = checker;
    this.emitter = emitter;
  }

  @Override
  public Double get() {
    return this.getter.get();
  }

  @Override
  public void add(final Double delta) {
    this.emitter.accept(delta);
  }

  @Override
  public Constraint when(final Predicate<Double> condition) {
    return Constraint.createStateConstraint(this.name, () -> this.checker.apply(condition));
  }

  @Override
  public Constraint whenGreaterThan(final Double y) {
    return this.when((x) -> (x > y));
  }

  @Override
  public Constraint whenLessThan(final Double y) {
    return this.when((x) -> (x < y));
  }

  @Override
  public Constraint whenLessThanOrEqualTo(final Double y) {
    return this.when((x) -> (x <= y));
  }

  @Override
  public Constraint whenGreaterThanOrEqualTo(final Double y) {
    return this.when((x) -> (x >= y));
  }

  @Override
  public Constraint whenEqualWithin(final Double y, final Double tolerance) {
    return this.when((x) -> (Math.abs(x - y) < tolerance));
  }
}
