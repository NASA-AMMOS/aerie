package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import java.util.Collections;
import java.util.Set;

public final class ViolableConstraint {
  public String id;
  public String name;
  public String message;
  public String category;

  public final Set<String> activityIds = Collections.emptySet();
  public final Set<String> activityTypes = Collections.emptySet();
  public final Set<String> stateIds = Collections.emptySet();
}
