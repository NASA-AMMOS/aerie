package gov.nasa.jpl.aerie.scheduler;

import java.util.LinkedList;
import java.util.List;

/**
 * Constraint representing a disjunction of constraints
 */
public class StateConstraintExpressionDisjunction extends StateConstraintExpression {

  public StateConstraintExpressionDisjunction(List<StateConstraintExpression> constraints) {
    this(constraints, null);
  }

  protected StateConstraintExpressionDisjunction(List<StateConstraintExpression> constraints, String name) {
    super(null, name);

    disjunction = new LinkedList<StateConstraintExpression>(constraints);
    cache = new ValidityCache() {
      @Override
      public TimeWindows fetchValue(Plan plan, TimeWindows intervals) {
        return findWindowsStates(plan, intervals);
      }
    };
  }


  public static boolean ACTIVATE_CACHE = true;

  private List<StateConstraintExpression> disjunction;

  private ValidityCache cache;

  public TimeWindows findWindowsStates(Plan plan, TimeWindows windows) {
    TimeWindows disjunctionTimeWindows = new TimeWindows();
    for (StateConstraintExpression c : disjunction) {
      TimeWindows tw = c.findWindows(plan, windows);
      disjunctionTimeWindows.union(tw);
    }

    return disjunctionTimeWindows;
  }

  /**
   * Finding windows for a disjunction is finding the union of the windows of the contributing constraints
   *
   * @param plan IN the plan context to evaluate the constraint in
   * @param windows IN the narrowed time ranges in the plan in which
   *     to search for constraint satisfiaction
   * @return the time ranges in which the disjunction of constraints is satisfied
   */
  @Override
  public TimeWindows findWindows(Plan plan, TimeWindows windows) {
    if (ACTIVATE_CACHE) {
      return cache.findWindowsCache(plan, windows);
    }
    return findWindowsStates(plan, windows);
  }


}
