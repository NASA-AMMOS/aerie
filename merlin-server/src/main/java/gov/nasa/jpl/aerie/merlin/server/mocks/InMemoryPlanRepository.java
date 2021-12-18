package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class InMemoryPlanRepository implements PlanRepository {
  private final Map<String, Pair<Long, Plan>> plans = new HashMap<>();
  private int nextPlanId = 0;
  private int nextActivityId = 0;

  @Override
  public Map<String, Plan> getAllPlans() {
    return this.plans
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> new Plan(entry.getValue().getRight())));
  }

  @Override
  public Plan getPlan(final String planId) throws NoSuchPlanException {
    final Plan plan = Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId))
        .getRight();

    return new Plan(plan);
  }

  @Override
  public long getPlanRevision(final String planId) throws NoSuchPlanException {
    return Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId))
        .getLeft();
  }

  @Override
  public Map<String, ActivityInstance> getAllActivitiesInPlan(final String planId) throws NoSuchPlanException {
    final Plan plan = this.plans.get(planId).getRight();
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    return plan.activityInstances
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            (entry) -> entry.getKey(),
            (entry) -> new ActivityInstance(entry.getValue())));
  }

  @Override
  public CreatedPlan createPlan(final NewPlan newPlan) {
    final String planId = Objects.toString(this.nextPlanId++);

    final Plan plan = new Plan();
    plan.name = newPlan.name;
    plan.startTimestamp = newPlan.startTimestamp;
    plan.endTimestamp = newPlan.endTimestamp;
    plan.configuration = newPlan.configuration;
    plan.missionModelId = newPlan.missionModelId;
    plan.activityInstances = new HashMap<>();

    final List<String> activityIds;
    if (newPlan.activityInstances == null) {
      activityIds = new ArrayList<>();
    } else {
      activityIds = new ArrayList<>(newPlan.activityInstances.size());
      for (final var activity : newPlan.activityInstances) {
        final String activityId = Objects.toString(this.nextActivityId++);

        activityIds.add(activityId);
        plan.activityInstances.put(activityId, new ActivityInstance(activity));
      }
    }

    this.plans.put(planId, Pair.of(0L, plan));

    return new CreatedPlan(planId, activityIds);
  }

  @Override
  public PlanTransaction updatePlan(final String id) {
    return new MockPlanTransaction(id);
  }

  @Override
  public void deletePlan(final String id) throws NoSuchPlanException {
    if (!this.plans.containsKey(id)) {
      throw new NoSuchPlanException(id);
    }

    this.deleteAllActivities(id);
    this.plans.remove(id);
  }

  @Override
  public String createActivity(final String planId, final ActivityInstance activity) throws NoSuchPlanException {
    final var entry = this.plans.get(planId);
    if (entry == null) throw new NoSuchPlanException(planId);

    final var plan = entry.getRight();
    final var revision = entry.getLeft() + 1;

    final String activityId = Objects.toString(this.nextActivityId++);
    plan.activityInstances.put(activityId, new ActivityInstance(activity));
    this.plans.put(planId, Pair.of(revision, plan));

    return activityId;
  }

  @Override
  public void deleteAllActivities(final String planId) throws NoSuchPlanException {
    final var entry = this.plans.get(planId);
    if (entry == null) throw new NoSuchPlanException(planId);

    final var plan = entry.getRight();
    final var revision = entry.getLeft() + 1;

    plan.activityInstances.clear();
    this.plans.put(planId, Pair.of(revision, plan));
  }

  @Override
  public Map<String, Constraint> getAllConstraintsInPlan(final String planId) throws NoSuchPlanException {
    return Map.of();
  }

  @Override
  public long addExternalDataset(final String planId, final Timestamp datasetStart, final ProfileSet profileSet)
  throws NoSuchPlanException
  {
    return 0;
  }

  private class MockPlanTransaction implements PlanTransaction {
    private final String planId;

    private Optional<String> name = Optional.empty();
    private Optional<Timestamp> startTimestamp = Optional.empty();
    private Optional<Timestamp> endTimestamp = Optional.empty();
    private Optional<Map<String, SerializedValue>> configuration = Optional.empty();
    private Optional<String> missionModelId = Optional.empty();

    public MockPlanTransaction(final String planId) {
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

  private class MockActivityTransaction implements ActivityTransaction {
    private final String planId;
    private final String activityId;

    private Optional<String> type = Optional.empty();
    private Optional<Timestamp> startTimestamp = Optional.empty();
    private Optional<Map<String, SerializedValue>> parameters = Optional.empty();

    public MockActivityTransaction(final String planId, final String activityId) {
      this.planId = planId;
      this.activityId = activityId;
    }

    @Override
    public void commit() throws NoSuchPlanException, NoSuchActivityInstanceException {
      final var entry = InMemoryPlanRepository.this.plans.get(this.planId);
      if (entry == null) throw new NoSuchPlanException(this.planId);

      final var plan = entry.getRight();
      final var revision = entry.getLeft() + 1;

      final ActivityInstance activity = plan.activityInstances.get(this.activityId);
      if (activity == null) {
        throw new NoSuchActivityInstanceException(this.planId, this.activityId);
      }

      this.type.ifPresent(type -> activity.type = type);
      this.startTimestamp.ifPresent(startTimestamp -> activity.startTimestamp = startTimestamp);
      this.parameters.ifPresent(arguments -> activity.arguments = arguments);

      InMemoryPlanRepository.this.plans.put(this.planId, Pair.of(revision, plan));
    }

    @Override
    public ActivityTransaction setType(final String type) {
      this.type = Optional.of(type);
      return this;
    }

    @Override
    public ActivityTransaction setStartTimestamp(final Timestamp timestamp) {
      this.startTimestamp = Optional.of(timestamp);
      return this;
    }

    @Override
    public ActivityTransaction setParameters(final Map<String, SerializedValue> parameters) {
      this.parameters = Optional.of(new HashMap<>(parameters));
      return this;
    }
  }
}
