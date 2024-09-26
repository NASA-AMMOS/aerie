package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public record SimulationData(
    Plan plan,
    SimulationResultsInterface driverResults,
    gov.nasa.jpl.aerie.constraints.model.SimulationResults constraintsResults
){}
