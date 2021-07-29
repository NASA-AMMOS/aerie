package gov.nasa.jpl.aerie.merlin.server.remotes.replicated;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReplicatedPlanRepository implements PlanRepository {
  /**
   * @param planIds An auxiliary repository mapping plan primary IDs to plan IDs in this secondary.
   * @param activityIds An auxiliary repository mapping activity primary IDs to activity IDs in this secondary.
   * @param repository The secondary repository itself.
   */
  public record Secondary (
      IdMappingRepository planIds,
      IdMappingRepository activityIds,
      PlanRepository repository
  ) {}

  private final PlanRepository primary;
  private final List<Secondary> secondaries;

  public ReplicatedPlanRepository(final PlanRepository primary, final List<Secondary> secondaries) {
    this.primary = primary;
    this.secondaries = new ArrayList<>(secondaries);
  }

  @Override
  public Map<String, Plan> getAllPlans() {
    final var result = this.primary.getAllPlans();

    for (final var secondary : this.secondaries) {
      secondary.repository().getAllPlans();
    }

    return result;
  }

  @Override
  public Plan getPlan(final String planId) throws NoSuchPlanException {
    final var result = this.primary.getPlan(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getPlan(secondary.planIds().lookup(planId));
    }

    return result;
  }

  @Override
  public long getPlanRevision(final String planId) throws NoSuchPlanException {
    final var result = this.primary.getPlanRevision(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getPlanRevision(secondary.planIds().lookup(planId));
    }

    return result;
  }

  @Override
  public Map<String, ActivityInstance> getAllActivitiesInPlan(final String planId) throws NoSuchPlanException {
    final var result = this.primary.getAllActivitiesInPlan(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getAllActivitiesInPlan(secondary.planIds().lookup(planId));
    }

    return result;
  }

  @Override
  public ActivityInstance getActivityInPlanById(final String planId, final String activityId)
  throws NoSuchPlanException, NoSuchActivityInstanceException
  {
    final var result = this.primary.getActivityInPlanById(planId, activityId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getActivityInPlanById(
          secondary.planIds().lookup(planId),
          secondary.activityIds().lookup(activityId));
    }

    return result;
  }

  @Override
  public CreatedPlan createPlan(final NewPlan plan) {
    final var primaryIds = this.primary.createPlan(plan);

    for (final var secondary : this.secondaries) {
      final var secondaryIds = secondary.repository().createPlan(plan);

      secondary.planIds().insert(primaryIds.planId(), secondaryIds.planId());
      // TODO: Check that there are as many primary activityIDs as secondary activityIDs.
      for (var i = 0; i < primaryIds.activityIds().size(); i += 1) {
        secondary.activityIds().insert(primaryIds.activityIds().get(i), secondaryIds.activityIds().get(i));
      }
    }

    return primaryIds;
  }

  @Override
  public PlanTransaction updatePlan(final String planId) throws NoSuchPlanException {
    final var primaryTransaction = this.primary.updatePlan(planId);

    final var secondaryTransactions = new ArrayList<PlanTransaction>(this.secondaries.size());
    for (final var secondary : this.secondaries) {
      secondaryTransactions.add(secondary.repository().updatePlan(secondary.planIds().lookup(planId)));
    }

    return new ReplicatedPlanTransaction(primaryTransaction, secondaryTransactions);
  }

  @Override
  public List<String> replacePlan(final String planId, final NewPlan plan) throws NoSuchPlanException {
    final var primaryIds = this.primary.replacePlan(planId, plan);

    for (final var secondary : this.secondaries) {
      final var secondaryIds = secondary.repository().replacePlan(secondary.planIds().lookup(planId), plan);

      // TODO: Check that there are as many primary activityIDs as secondary activityIDs.
      for (var i = 0; i < primaryIds.size(); i += 1) {
        secondary.activityIds().insert(primaryIds.get(i), secondaryIds.get(i));
      }
    }

    return primaryIds;
  }

  @Override
  public void deletePlan(final String planId) throws NoSuchPlanException {
    // TODO: Consider returning the set of activity IDs deleted alongside the given plan ID.
    //   (In general, consider returning any IDs whose deletion is implied by deletion of a given ID.)
    //   This would let us avoid an explicit query and extra round-trip, allowing clients (and ourselves)
    //   to propagate deletion of entities through their internal reflection of database state.
    final var activityIds = this.primary.getAllActivitiesInPlan(planId).keySet();
    this.primary.deletePlan(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().deletePlan(secondary.planIds().lookup(planId));
      secondary.planIds().delete(planId);
      for (final var activityId : activityIds) {
        secondary.activityIds().delete(activityId);
      }
    }
  }

  @Override
  public String createActivity(final String planId, final ActivityInstance activity) throws NoSuchPlanException {
    final var activityId = this.primary.createActivity(planId, activity);

    for (final var secondary : this.secondaries) {
      final var secondaryId = secondary.repository().createActivity(secondary.planIds().lookup(planId), activity);

      secondary.activityIds().insert(activityId, secondaryId);
    }

    return activityId;
  }

  @Override
  public ActivityTransaction updateActivity(final String planId, final String activityId)
  throws NoSuchPlanException, NoSuchActivityInstanceException
  {
    final var primaryTransaction = this.primary.updateActivity(planId, activityId);

    final var secondaryTransactions = new ArrayList<ActivityTransaction>(this.secondaries.size());
    for (final var secondary : this.secondaries) {
      secondaryTransactions.add(secondary.repository().updateActivity(
          secondary.planIds().lookup(planId),
          secondary.activityIds().lookup(activityId)));
    }

    return new ReplicatedActivityTransaction(primaryTransaction, secondaryTransactions);
  }

  @Override
  public void replaceActivity(final String planId, final String activityId, final ActivityInstance activity)
  throws NoSuchPlanException, NoSuchActivityInstanceException
  {
    this.primary.replaceActivity(planId, activityId, activity);

    for (final var secondary : this.secondaries) {
      secondary.repository().replaceActivity(
          secondary.planIds().lookup(planId),
          secondary.activityIds().lookup(activityId),
          activity);
    }
  }

  @Override
  public void deleteActivity(final String planId, final String activityId)
  throws NoSuchPlanException, NoSuchActivityInstanceException
  {
    this.primary.deleteActivity(planId, activityId);

    for (final var secondary : this.secondaries) {
      secondary.repository().deleteActivity(
          secondary.planIds().lookup(planId),
          secondary.activityIds().lookup(activityId));
    }
  }

  @Override
  public void deleteAllActivities(final String planId) throws NoSuchPlanException {
    // TODO: Consider returning the set of activity IDs deleted.
    //   (In general, consider returning any IDs whose deletion is implicit.)
    //   This would let us avoid an explicit query and extra round-trip, allowing clients (and ourselves)
    //   to propagate deletion of entities through their internal reflection of database state.
    final var activityIds = this.primary.getAllActivitiesInPlan(planId).keySet();
    this.primary.deleteAllActivities(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().deleteAllActivities(secondary.planIds().lookup(planId));
      for (final var activityId : activityIds) {
        secondary.activityIds().delete(activityId);
      }
    }
  }

  @Override
  public Map<String, Constraint> getAllConstraintsInPlan(final String planId) throws NoSuchPlanException {
    final var results = this.primary.getAllConstraintsInPlan(planId);

    for (final var secondary : this.secondaries) {
      secondary.repository().getAllConstraintsInPlan(secondary.planIds().lookup(planId));
    }

    return results;
  }

  @Override
  public void replacePlanConstraints(final String planId, final Map<String, Constraint> constraints)
  throws NoSuchPlanException
  {
    this.primary.replacePlanConstraints(planId, constraints);

    for (final var secondary : this.secondaries) {
      secondary.repository().replacePlanConstraints(secondary.planIds().lookup(planId), constraints);
    }
  }

  @Override
  public void deleteConstraintInPlanById(final String planId, final String constraintId) throws NoSuchPlanException {
    this.primary.deleteConstraintInPlanById(planId, constraintId);

    for (final var secondary : this.secondaries) {
      secondary.repository().deleteConstraintInPlanById(secondary.planIds().lookup(planId), constraintId);
    }
  }

  private record ReplicatedPlanTransaction(
      PlanTransaction primaryTransaction,
      List<PlanTransaction> secondaryTransactions
  ) implements PlanTransaction {
    @Override
    public void commit() throws NoSuchPlanException {
      this.primaryTransaction.commit();

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.commit();
      }
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.primaryTransaction.setName(name);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setName(name);
      }

      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final Timestamp timestamp) {
      this.primaryTransaction.setStartTimestamp(timestamp);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setStartTimestamp(timestamp);
      }

      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final Timestamp timestamp) {
      this.primaryTransaction.setEndTimestamp(timestamp);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setEndTimestamp(timestamp);
      }

      return this;
    }

    @Override
    public PlanTransaction setConfiguration(final Map<String, SerializedValue> configuration) {
      this.primaryTransaction.setConfiguration(configuration);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setConfiguration(configuration);
      }

      return this;
    }

    @Override
    public PlanTransaction setAdaptationId(final String adaptationId) {
      this.primaryTransaction.setAdaptationId(adaptationId);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setAdaptationId(adaptationId);
      }

      return this;
    }
  }

  private record ReplicatedActivityTransaction(
      ActivityTransaction primaryTransaction,
      List<ActivityTransaction> secondaryTransactions
  ) implements ActivityTransaction {
    @Override
    public void commit() throws NoSuchPlanException, NoSuchActivityInstanceException {
      this.primaryTransaction.commit();

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.commit();
      }
    }

    @Override
    public ActivityTransaction setType(final String type) {
      this.primaryTransaction.setType(type);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setType(type);
      }

      return this;
    }

    @Override
    public ActivityTransaction setStartTimestamp(final Timestamp timestamp) {
      this.primaryTransaction.setStartTimestamp(timestamp);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setStartTimestamp(timestamp);
      }

      return this;
    }

    @Override
    public ActivityTransaction setParameters(final Map<String, SerializedValue> parameters) {
      this.primaryTransaction.setParameters(parameters);

      for (final var secondaryTransaction : this.secondaryTransactions) {
        secondaryTransaction.setParameters(parameters);
      }

      return this;
    }
  }
}
