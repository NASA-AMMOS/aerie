package gov.nasa.jpl.aerie.merlin.framework.htn;

import java.util.Set;

public interface Method<T> {
  Set<ActivityInstantiation> getSubtasks(final T instance);
  Set<TaskNetworkConstraint> getConstraints(final T instance);
}
