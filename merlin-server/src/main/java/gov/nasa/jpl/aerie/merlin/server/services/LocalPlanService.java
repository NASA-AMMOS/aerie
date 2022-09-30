package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final class LocalPlanService implements PlanService {
  private final PlanRepository planRepository;

  public LocalPlanService(
      final PlanRepository planRepository
  ) {
    this.planRepository = planRepository;
  }

  @Override
  public Plan getPlan(final PlanId planId) throws NoSuchPlanException {
    return this.planRepository.getPlan(planId);
  }

  @Override
  public RevisionData getPlanRevisionData(final PlanId planId) throws NoSuchPlanException {
    return this.planRepository.getPlanRevisionData(planId);
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final PlanId planId) throws NoSuchPlanException {
    return this.planRepository.getAllConstraintsInPlan(planId);
  }

  @Override
  public long addExternalDataset(final PlanId planId, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return this.planRepository.addExternalDataset(planId, datasetStart, profileSet);
  }

  @Override
  public List<Pair<Duration, ProfileSet>> getExternalDatasets(final PlanId planId) throws NoSuchPlanException {
    return this.planRepository.getExternalDatasets(planId);
  }

  @Override
  public Map<String, ValueSchema> getExternalResourceSchemas(final PlanId planId) throws NoSuchPlanException {
    return this.planRepository.getExternalResourceSchemas(planId);
  }
}
