package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;

import java.util.List;

public class GlobalConstraints {

  public static NAryMutexConstraint atMostOneOf(ActivityExpression... types) {
    return new NAryMutexConstraint(types);
  }
  public static BinaryMutexConstraint buildBinaryMutexConstraint(ActivityType type1, ActivityType type2) {
    BinaryMutexConstraint mc = new BinaryMutexConstraint();
    mc.fill(type1, type2);
    return mc;
  }

}
