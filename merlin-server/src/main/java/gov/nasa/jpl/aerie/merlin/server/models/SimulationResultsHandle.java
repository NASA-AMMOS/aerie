package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;

public class SimulationResultsHandle {

  private final SimulationResults simulationResults;

  public SimulationResultsHandle(SimulationResults simulationResults) {
    this.simulationResults = simulationResults;
  }

  public SimulationResults getSimulationResults() {
    return this.simulationResults;
  }
}
