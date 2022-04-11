package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;

import java.util.List;

public class GlobalConstraints {

  public static NAryMutexConstraint atMostOneOf(List<ActivityExpression> types) {
    return NAryMutexConstraint.buildMutexConstraint(types);
  }

}
