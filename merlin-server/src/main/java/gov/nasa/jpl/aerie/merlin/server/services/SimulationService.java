package gov.nasa.jpl.aerie.merlin.server.services;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.apache.commons.lang3.tuple.Pair;

public interface SimulationService {
  ResultsProtocol.State getSimulationResults(PlanId planId, RevisionData revisionData);
  Map<String, List<Pair<Duration, SerializedValue>>> getResourceSamples(PlanId planId, RevisionData revisionData);
}
