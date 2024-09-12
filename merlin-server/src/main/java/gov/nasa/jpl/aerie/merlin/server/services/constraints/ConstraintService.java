package gov.nasa.jpl.aerie.merlin.server.services.constraints;

import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.Map;

public interface ConstraintService {
  void createConstraintRuns(Map<Long, Constraint> constraintMap, Map<Long, ConstraintResult> constraintResults, SimulationDatasetId simulationDatasetId);
  Map<Long, ConstraintRunRecord> getValidConstraintRuns(Map<Long,Constraint> constraints, SimulationDatasetId simulationDatasetId);
}
