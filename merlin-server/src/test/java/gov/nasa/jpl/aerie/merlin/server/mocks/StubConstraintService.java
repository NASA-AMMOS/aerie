package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;
import gov.nasa.jpl.aerie.merlin.server.services.ConstraintService;

import java.util.List;
import java.util.Map;

public class StubConstraintService implements ConstraintService {
  @Override
  public void createConstraintRuns(
      final Map<Long, Constraint> constraintMap,
      final Map<Long, Violation> violations,
      final SimulationDatasetId simulationDatasetId)
  {

  }

  @Override
  public Map<Long, ConstraintRunRecord> getPreviouslyResolvedConstraints(final List<Constraint> constraints) {
    return null;
  }
}
