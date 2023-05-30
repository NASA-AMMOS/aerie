package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.List;

public interface ConstraintRepository {
  void insertConstraintRuns(final List<Violation> violations);

  List<ConstraintRunRecord> getConstraintRuns(List<Long> constraintIds);
}
