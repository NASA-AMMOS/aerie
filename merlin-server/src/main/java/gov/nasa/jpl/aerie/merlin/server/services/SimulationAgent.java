package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public interface SimulationAgent {
  void simulate(String planId, RevisionData revisionData, ResultsProtocol.WriterRole writer) throws InterruptedException;
}
