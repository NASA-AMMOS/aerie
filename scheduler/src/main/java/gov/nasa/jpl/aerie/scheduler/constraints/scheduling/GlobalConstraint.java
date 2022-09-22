package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * Interface defining methods that must be implemented by global constraints such as mutex or cardinality
 * Also provides a directory for creating these constraints
 */
public interface GlobalConstraint {

  //todo: probably needs a domain

  //is the constraint enforced on its domain
  ConstraintState isEnforced(Plan plan, Windows windows, SimulationResults simulationResults);


}
