package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class InMemoryPlanRepository implements PlanRepository {
  private final Map<PlanId, Pair<Long, Plan>> plans = new HashMap<>();
  private int nextPlanId = 0;
  private int nextActivityId = 0;

  @Override
  public Map<PlanId, Plan> getAllPlans() {
    return this.plans
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> new Plan(entry.getValue().getRight())));
  }

  @Override
  public Plan getPlan(final PlanId planId) throws NoSuchPlanException {
    final Plan plan = Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId))
        .getRight();

    return new Plan(plan);
  }

  @Override
  public long getPlanRevision(final PlanId planId) throws NoSuchPlanException {
    return Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId))
        .getLeft();
  }

  @Override
  public InMemoryRevisionData getPlanRevisionData(final PlanId planId) throws NoSuchPlanException {
    return new InMemoryRevisionData(
        Optional
            .ofNullable(this.plans.get(planId))
            .orElseThrow(() -> new NoSuchPlanException(planId))
            .getLeft());
  }

  @Override
  public Map<ActivityDirectiveId, ActivityDirective> getAllActivitiesInPlan(final PlanId planId) throws NoSuchPlanException {
    final Plan plan = this.plans.get(planId).getRight();
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    return new HashMap<>(plan.activityDirectives);
  }

  public CreatedPlan createPlan(final NewPlan newPlan) {
    final PlanId planId = new PlanId(this.nextPlanId++);

    final Plan plan = new Plan();
    plan.name = newPlan.name;
    plan.startTimestamp = newPlan.startTimestamp;
    plan.endTimestamp = newPlan.endTimestamp;
    plan.configuration = newPlan.configuration;
    plan.missionModelId = newPlan.missionModelId;
    plan.activityDirectives = new HashMap<>();

    final List<ActivityDirectiveId> activityIds;
    if (newPlan.activityDirectives == null) {
      activityIds = new ArrayList<>();
    } else {
      activityIds = new ArrayList<>(newPlan.activityDirectives.size());
      for (final var activity : newPlan.activityDirectives) {
        final ActivityDirectiveId activityId = new ActivityDirectiveId(this.nextActivityId++);

        activityIds.add(activityId);
        plan.activityDirectives.put(activityId, activity);
      }
    }

    this.plans.put(planId, Pair.of(0L, plan));

    return new CreatedPlan(planId, activityIds);
  }

  public PlanTransaction updatePlan(final PlanId planId) {
    return new MockPlanTransaction(planId);
  }

  public void deletePlan(final PlanId planId) throws NoSuchPlanException {
    if (!this.plans.containsKey(planId)) {
      throw new NoSuchPlanException(planId);
    }

    this.deleteAllActivities(planId);
    this.plans.remove(planId);
  }

  public ActivityDirectiveId createActivity(final PlanId planId, final ActivityDirective activity) throws NoSuchPlanException {
    final var entry = this.plans.get(planId);
    if (entry == null) throw new NoSuchPlanException(planId);

    final var plan = entry.getRight();
    final var revision = entry.getLeft() + 1;

    final ActivityDirectiveId activityId = new ActivityDirectiveId(this.nextActivityId++);
    plan.activityDirectives.put(activityId, activity);
    this.plans.put(planId, Pair.of(revision, plan));

    return activityId;
  }

  public void deleteAllActivities(final PlanId planId) throws NoSuchPlanException {
    final var entry = this.plans.get(planId);
    if (entry == null) throw new NoSuchPlanException(planId);

    final var plan = entry.getRight();
    final var revision = entry.getLeft() + 1;

    plan.activityDirectives.clear();
    this.plans.put(planId, Pair.of(revision, plan));
  }

  @Override
  public Map<String, Constraint> getAllConstraintsInPlan(final PlanId planId) throws NoSuchPlanException {
    return Map.of();
  }

  @Override
  public long addExternalDataset(final PlanId planId, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return 0;
  }

  @Override
  public List<Pair<Duration, ProfileSet>> getExternalDatasets(final PlanId planId) throws NoSuchPlanException {
    return List.of();
  }

  @Override
  public Map<String, ValueSchema> getExternalResourceSchemas(final PlanId planId) throws NoSuchPlanException {
    return Map.of();
  }

  private class MockPlanTransaction implements PlanTransaction {
    private final PlanId planId;

    private Optional<String> name = Optional.empty();
    private Optional<Timestamp> startTimestamp = Optional.empty();
    private Optional<Timestamp> endTimestamp = Optional.empty();
    private Optional<Map<String, SerializedValue>> configuration = Optional.empty();
    private Optional<String> missionModelId = Optional.empty();

    public MockPlanTransaction(final PlanId planId) {
      this.planId = planId;
    }

    @Override
    public void commit() throws NoSuchPlanException {
      final var entry = InMemoryPlanRepository.this.plans.get(this.planId);
      if (entry == null) throw new NoSuchPlanException(this.planId);

      final var plan = entry.getRight();
      final var revision = entry.getLeft() + 1;

      this.name.ifPresent(name -> plan.name = name);
      this.startTimestamp.ifPresent(startTimestamp -> plan.startTimestamp = startTimestamp);
      this.endTimestamp.ifPresent(endTimestamp -> plan.endTimestamp = endTimestamp);
      this.configuration.ifPresent(configuration -> plan.configuration = configuration);
      this.missionModelId.ifPresent(missionModelId -> plan.missionModelId = missionModelId);

      InMemoryPlanRepository.this.plans.put(this.planId, Pair.of(revision, plan));
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.name = Optional.of(name);
      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final Timestamp timestamp) {
      this.startTimestamp = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final Timestamp timestamp) {
      this.endTimestamp = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setConfiguration(final Map<String, SerializedValue> configuration)
    {
      this.configuration = Optional.of(configuration);
      return this;
    }
  }
}
