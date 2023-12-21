package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public interface SimulationAgent {
  void simulate(PlanId planId, RevisionData revisionData, ResultsProtocol.WriterRole writer) throws InterruptedException;
}
