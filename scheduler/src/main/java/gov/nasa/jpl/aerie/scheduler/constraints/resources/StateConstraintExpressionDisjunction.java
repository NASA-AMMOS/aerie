package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.LinkedList;
import java.util.List;

/**
 * Constraint representing a disjunction of constraints
 */
public class StateConstraintExpressionDisjunction extends StateConstraintExpression {

  public StateConstraintExpressionDisjunction(List<StateConstraintExpression> constraints) {
    this(constraints, null);
  }

  public StateConstraintExpressionDisjunction(List<StateConstraintExpression> constraints, String name) {
    super(null, name);

    disjunction = new LinkedList<>(constraints);
    cache = new ValidityCache() {
      @Override
      public Windows fetchValue(Plan plan, Windows intervals) {
        return findWindowsStates(plan, intervals);
      }
    };
  }


  public final static boolean ACTIVATE_CACHE = false;

  private final List<StateConstraintExpression> disjunction;

  private final ValidityCache cache;

  public Windows findWindowsStates(Plan plan, Windows windows) {
    Windows disjunctionTimeWindows = new Windows();
    for (StateConstraintExpression c : disjunction) {
      Windows tw = c.findWindows(plan, windows);
      disjunctionTimeWindows.addAll(tw);
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
  public Windows findWindows(Plan plan, Windows windows) {
    if (ACTIVATE_CACHE) {
      return cache.findWindowsCache(plan, windows);
    }
    return findWindowsStates(plan, windows);
  }


}
