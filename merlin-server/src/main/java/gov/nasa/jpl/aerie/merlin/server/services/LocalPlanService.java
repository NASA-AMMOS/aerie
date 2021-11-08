package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import java.util.Map;

public final class LocalPlanService implements PlanService {
  private final PlanRepository planRepository;

  public LocalPlanService(
      final PlanRepository planRepository
  ) {
    this.planRepository = planRepository;
  }

  @Override
  public Plan getPlanById(final String id) throws NoSuchPlanException {
    return this.planRepository.getPlan(id);
  }

  @Override
  public long getPlanRevisionById(final String id) throws NoSuchPlanException {
    return this.planRepository.getPlanRevision(id);
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final String planId) throws NoSuchPlanException {
    return this.planRepository.getAllConstraintsInPlan(planId);
  }

}
