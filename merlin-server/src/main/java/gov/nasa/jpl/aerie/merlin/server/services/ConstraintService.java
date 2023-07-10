package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.List;
import java.util.Map;

public interface ConstraintService {
  void createConstraintRuns(Map<Long, Constraint> constraintMap, Map<Long, Violation> violations, SimulationDatasetId simulationDatasetId);
  Map<Long, ConstraintRunRecord> getPreviouslyResolvedConstraints(List<Constraint> constraints);
}
