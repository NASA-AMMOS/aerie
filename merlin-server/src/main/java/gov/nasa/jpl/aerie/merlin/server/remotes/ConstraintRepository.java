package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.List;
import java.util.Map;

public interface ConstraintRepository {
  void insertConstraintRuns(final Map<Long, Constraint> constraintMap, final Map<Long, Violation> violations,
                            final PlanId planId, final Long simulationDatasetId);

  List<ConstraintRunRecord> getSuccessfulConstraintRuns(List<Long> constraintIds);
}
