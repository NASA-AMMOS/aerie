package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public interface SimulationAgent {
  void simulate(String planId, long planRevision, ResultsProtocol.WriterRole writer) throws InterruptedException;
}
