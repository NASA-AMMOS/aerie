package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * Above state constraint
 *
 */
public class StateConstraintAbove extends StateConstraint {

  protected StateConstraintAbove() {
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
    return this.state.whenValueAbove(this.valueDefinition.get(0), windows);
  }


}
