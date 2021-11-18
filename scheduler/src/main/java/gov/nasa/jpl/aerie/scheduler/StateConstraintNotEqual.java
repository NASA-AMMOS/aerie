package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * Equal state constraint
 *
 * @param <T> the type of the state on which the constraint applies
 */
public class StateConstraintNotEqual<T extends Comparable<T>> extends StateConstraint<T> {

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
    Windows wins = this.state.whenValueNotEqual(this.valueDefinition.get(0), windows);
    return wins;
  }


}
