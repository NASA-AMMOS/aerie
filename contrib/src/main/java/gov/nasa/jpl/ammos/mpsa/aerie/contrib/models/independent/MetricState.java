package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;

import java.util.function.Predicate;

public interface MetricState<T extends Comparable<T>, Delta> {
  T get();
  void add(Delta delta);

  Constraint when(final Predicate<T> condition, ConditionTypes.StateComparator type, T value);
  Constraint whenGreaterThan(final T y);
  Constraint whenLessThan(final T y);
  Constraint whenLessThanOrEqualTo(final T y);
  Constraint whenGreaterThanOrEqualTo(final T y);
  Constraint whenEqualWithin(final T y, final Delta tolerance);
}
