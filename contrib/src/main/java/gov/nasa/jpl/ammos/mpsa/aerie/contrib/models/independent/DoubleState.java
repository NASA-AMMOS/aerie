package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes.StateComparator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
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
  )
  {
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

  // TODO: This allows adaptation engineers to construct constraints with a structure that's not tied to the condition of the constraint. We will need to rework this to not allow this situation to occur.
  @Override
  public Constraint when(final Predicate<Double> condition, StateComparator comparator, Double value) {
    return Constraint.createStateConstraint(this.name, () -> this.checker.apply(condition),
                                            ConstraintStructure.ofStateConstraint(
                                                this.name, comparator, SerializedValue.of(value)));
  }

  @Override
  public Constraint whenGreaterThan(final Double y) {
    return this.when((x) -> (x > y), StateComparator.GREATER_THAN, y);
  }

  @Override
  public Constraint whenLessThan(final Double y) {
    return this.when((x) -> (x < y), StateComparator.LESS_THAN, y);
  }

  @Override
  public Constraint whenLessThanOrEqualTo(final Double y) {
    return this.when((x) -> (x <= y), StateComparator.LESS_THAN_OR_EQUAL_TO, y);
  }

  @Override
  public Constraint whenGreaterThanOrEqualTo(final Double y) {
    return this.when((x) -> (x >= y), StateComparator.GREATER_THAN_OR_EQUAL_TO, y);
  }

  @Override
  public Constraint whenEqualWithin(final Double y, final Double tolerance) {
    return this.when((x) -> (Math.abs(x - y) < tolerance), StateComparator.EQUAL_WITHIN, y);
  }
}
