package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.List;
import java.util.Map;

public interface ConstraintService {
  void createConstraintRuns(Map<Long, Constraint> constraintMap, Map<Long, ConstraintResult> constraintResults, SimulationDatasetId simulationDatasetId);
  Map<Long, ConstraintRunRecord> getValidConstraintRuns(List<Constraint> constraints, SimulationDatasetId simulationDatasetId);
}
