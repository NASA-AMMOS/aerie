package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

public class GlobalConstraints {

  public static NAryMutexConstraint atMostOneOf(List<ActivityExpression> types) {
    return NAryMutexConstraint.buildMutexConstraint(types);
  }

}
