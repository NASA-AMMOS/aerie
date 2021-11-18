package gov.nasa.jpl.aerie.scheduler;

import java.util.LinkedList;
import java.util.List;

/**
 * Constraint representing a conjunction of other constraints
 */
public class StateConstraintExpressionConjunction extends StateConstraintExpression {


  protected StateConstraintExpressionConjunction(List<StateConstraintExpression> constraints, String name) {
    super(null, name);
    conjonction = new LinkedList<StateConstraintExpression>(constraints);
    cache = new ValidityCache() {
      @Override
      public TimeWindows fetchValue(Plan plan, TimeWindows intervals) {
        return findWindowsStates(plan, intervals);
      }
    };
  }

  private ValidityCache cache;

  public static boolean ACTIVATE_CACHE = false;

  private List<StateConstraintExpression> conjonction;

  /**
   * Finding a window for a conjunction is finding the intersection of the windows of all the contributing constraints
   *
   * @param plan IN the plan context to evaluate the constraint in
   * @param windows IN the narrowed time ranges in the plan in which
   *     to search for constraint satisfiaction
   * @return x
   */
  @Override
  public TimeWindows findWindows(Plan plan, TimeWindows windows) {
    if (ACTIVATE_CACHE) {
      return cache.findWindowsCache(plan, windows);
    }
    return findWindowsStates(plan, windows);

  }

  public TimeWindows findWindowsStates(Plan plan, TimeWindows windows) {
    TimeWindows returnedWindows = new TimeWindows(windows);
    for (StateConstraintExpression st : conjonction) {
      returnedWindows = st.findWindows(plan, returnedWindows);
      if (returnedWindows.isEmpty()) {
        break;
      }
    }
    return returnedWindows;
  }

  /**
   * Simplification and verification of consistency (no contradictory statements)
   * TODO:implement
   */
  protected void reduce() {
  }

}
