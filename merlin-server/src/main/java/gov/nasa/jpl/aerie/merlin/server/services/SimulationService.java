package gov.nasa.jpl.aerie.merlin.server.services;

import java.util.Optional;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public interface SimulationService {
  ResultsProtocol.State getSimulationResults(PlanId planId, RevisionData revisionData);
  Optional<SimulationResultsInterface> get(PlanId planId, RevisionData revisionData);
}
