package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

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
  public static boolean ACTIVATE_CACHE = false;

  protected Windows timeDomain;

  public Windows findWindows(Plan plan, Windows windows) {
    restrictToTimeDomain(windows);
    if (ACTIVATE_CACHE) {
      return new Windows(cache.findWindowsCache(plan, windows));
    }
    return findWindowsPart(plan, windows);
  }

  public abstract Windows findWindowsPart(Plan plan, Windows windows);

  public void setTimeDomain(Windows timeDomain) {
    this.timeDomain = timeDomain;
  }

  public void restrictToTimeDomain(Windows windows) {
    if (hasTimeDomain()) {
      windows.intersectWith(timeDomain);
    }
  }

  public boolean hasTimeDomain() {
    return this.timeDomain != null;
  }

  public Windows getTimeDomain() {
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
