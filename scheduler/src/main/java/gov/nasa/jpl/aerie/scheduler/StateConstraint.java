package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public abstract class StateConstraint<T extends Comparable<T>> {
  /**
   * List of values used to define the constraint
   */
  List<T> valueDefinition;
  /**
   * State to which the constraint refers to
   */
  ExternalState<T> state;

  /**
   * Value cache for the constraint preventing from querying distant state if value has already been queried
   */
  ValidityCache cache;

  protected StateConstraint() {

  }

  /**
   * Static boolean allowing to enable caching or not
   */
  public static boolean ACTIVATE_CACHE = true;

  protected TimeWindows timeDomain;

  public TimeWindows findWindows(Plan plan, TimeWindows windows) {
    restrictToTimeDomain(windows);
    if (ACTIVATE_CACHE) {
      return new TimeWindows(cache.findWindowsCache(plan, windows));
    }
    return findWindowsPart(plan, windows);
  }

  public abstract TimeWindows findWindowsPart(Plan plan, TimeWindows windows);

  public void setTimeDomain(TimeWindows timeDomain) {
    this.timeDomain = timeDomain;
  }

  public void restrictToTimeDomain(TimeWindows windows) {
    if (hasTimeDomain()) {
      windows.intersection(timeDomain);
    }
  }

  public boolean hasTimeDomain() {
    return this.timeDomain != null;
  }

  public TimeWindows getTimeDomain() {
    return this.timeDomain;
  }


  protected void setDomainUnary(T value) {
    this.valueDefinition = new ArrayList<T>();
    this.valueDefinition.add(value);
  }

  protected void setValueDefinition(List<T> values) {
    this.valueDefinition = values;
  }

  protected void setState(ExternalState<T> state) {
    this.state = state;
  }

}
