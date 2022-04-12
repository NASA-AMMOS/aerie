package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * Equal state constraint
 *
 */
public class StateConstraintNotEqual extends StateConstraint {

  protected StateConstraintNotEqual() {
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
    return this.state.whenValueNotEqual(this.valueDefinition.get(0), windows);
  }


}
