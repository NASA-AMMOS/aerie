package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

public interface SimulationService {
  ResultsProtocol.State getSimulationResults(String planId, RevisionData revisionData);
}
