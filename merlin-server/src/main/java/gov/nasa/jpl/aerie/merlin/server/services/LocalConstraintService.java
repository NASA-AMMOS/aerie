package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
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
  public Map<Long, ConstraintRunRecord> getPreviouslyResolvedConstraints(List<Constraint> constraints) {
    final var resolvedConstraintRuns = new HashMap<Long, ConstraintRunRecord>();
    final var constraintIds = constraints.stream().map(Constraint::id).collect(Collectors.toList());
    final var constraintRuns = constraintRepository.getConstraintRuns(constraintIds);

    for (final var constraintRun : constraintRuns) {
      resolvedConstraintRuns.put(constraintRun.constraintId(), constraintRun);
    }

    return resolvedConstraintRuns;
  }
}
