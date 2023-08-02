package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.List;
import java.util.Map;

public interface ConstraintRepository {
  void insertConstraintRuns(final Map<Long, Constraint> constraintMap, final Map<Long, ConstraintResult> violations,
                            final Long simulationDatasetId);

  Map<Long, ConstraintRunRecord> getValidConstraintRuns(List<Long> constraintIds, SimulationDatasetId simulationDatasetId);
}
