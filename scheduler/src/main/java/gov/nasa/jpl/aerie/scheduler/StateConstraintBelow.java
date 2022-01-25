package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * Above state constraint
 */
public class StateConstraintBelow extends StateConstraint {

  protected StateConstraintBelow() {
    cache = new ValidityCache() {
      @Override
      public Windows fetchValue(Plan plan, Windows intervals) {
        return findWindowsPart(plan, intervals);
      }
    };
  }

  /**
   * Facade for state interface
   *
   * @param plan IN current plan
   * @param windows IN set of time ranges in which search is performed
   * @return a set of time ranges in which the constraint is satisfied
   */
  public Windows findWindowsPart(Plan plan, Windows windows) {
    Windows wins = this.state.whenValueBelow(this.valueDefinition.get(0), windows);
    return wins;
  }


}
