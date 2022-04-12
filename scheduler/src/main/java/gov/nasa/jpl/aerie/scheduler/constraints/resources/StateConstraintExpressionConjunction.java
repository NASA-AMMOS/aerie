package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.LinkedList;
import java.util.List;

/**
 * Constraint representing a conjunction of other constraints
 */
public class StateConstraintExpressionConjunction extends StateConstraintExpression {


  protected StateConstraintExpressionConjunction(List<StateConstraintExpression> constraints, String name) {
    super(null, name);
    conjonction = new LinkedList<>(constraints);
    cache = new ValidityCache() {
      @Override
      public Windows fetchValue(Plan plan, Windows intervals) {
        return findWindowsStates(plan, intervals);
      }
    };
  }

  private final ValidityCache cache;

  public static final boolean ACTIVATE_CACHE = false;

  private final List<StateConstraintExpression> conjonction;

  /**
   * Finding a window for a conjunction is finding the intersection of the windows of all the contributing constraints
   *
   * @param plan IN the plan context to evaluate the constraint in
   * @param windows IN the narrowed time ranges in the plan in which
   *     to search for constraint satisfiaction
   * @return x
   */
  @Override
  public Windows findWindows(Plan plan, Windows windows) {
    if (ACTIVATE_CACHE) {
      return cache.findWindowsCache(plan, windows);
    }
    return findWindowsStates(plan, windows);

  }

  public Windows findWindowsStates(Plan plan, Windows windows) {
    Windows returnedWindows = new Windows(windows);
    for (StateConstraintExpression st : conjonction) {
      returnedWindows = st.findWindows(plan, returnedWindows);
      if (returnedWindows.isEmpty()) {
        break;
      }
    }
    return returnedWindows;
  }
}
