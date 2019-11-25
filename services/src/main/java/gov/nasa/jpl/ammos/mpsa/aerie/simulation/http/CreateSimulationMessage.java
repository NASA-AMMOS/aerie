package gov.nasa.jpl.ammos.mpsa.aerie.simulation.http;

public final class CreateSimulationMessage {
  public final String planId;

  public CreateSimulationMessage(final String planId) {
    this.planId = planId;
  }
}
