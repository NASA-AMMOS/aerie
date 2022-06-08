package gov.nasa.jpl.aerie.merlin.server.services;

import java.util.Optional;
import java.util.function.Function;

import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public interface SimulationService {
  ResultsProtocol.State getSimulationResults(PlanId planId, RevisionData revisionData);
  <T> Optional<T> get(PlanId planId, RevisionData revisionData, Function<SimulationResults, T> getter);
}
