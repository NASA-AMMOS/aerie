package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.remotes.ConstraintRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalConstraintService implements ConstraintService {
  private final ConstraintRepository constraintRepository;

  public LocalConstraintService(
    final ConstraintRepository constraintRepository
  ) {
    this.constraintRepository = constraintRepository;
  }

  @Override
  public void createConstraintRuns(final Map<Long, Constraint> constraintMap, final Map<Long, Violation> violations, final SimulationDatasetId simulationDatasetId) {
    this.constraintRepository.insertConstraintRuns(constraintMap, violations, simulationDatasetId.id());
  }

  @Override
  public Map<Long, ConstraintRunRecord> getValidConstraintRuns(List<Constraint> constraints, SimulationDatasetId simulationDatasetId) {
    return constraintRepository.getValidConstraintRuns(constraints.stream().map(Constraint::id).toList(), simulationDatasetId);
  }
}
