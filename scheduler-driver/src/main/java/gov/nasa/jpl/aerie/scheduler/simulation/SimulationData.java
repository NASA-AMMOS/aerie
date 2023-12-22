package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;

import java.util.Collection;

public record SimulationData(
    SimulationResultsInterface driverResults,
    gov.nasa.jpl.aerie.constraints.model.SimulationResults constraintsResults
){}
