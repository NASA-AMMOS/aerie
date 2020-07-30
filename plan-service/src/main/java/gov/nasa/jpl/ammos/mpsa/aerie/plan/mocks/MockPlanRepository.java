package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class MockPlanRepository implements PlanRepository {
  private final Map<String, Plan> plans = new HashMap<>();
  private int nextPlanId = 0;
  private int nextActivityId = 0;

  @Override
  public Stream<Pair<String, Plan>> getAllPlans() {
    return this.plans
        .entrySet()
        .stream()
        .map(entry -> Pair.of(entry.getKey(), new Plan(entry.getValue())));
  }

  @Override
  public Plan getPlan(final String planId) throws NoSuchPlanException {
    final Plan plan = Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId));

    return new Plan(plan);
  }

  @Override
  public Stream<Pair<String, ActivityInstance>> getAllActivitiesInPlan(final String planId) throws NoSuchPlanException {
    final Plan plan = this.plans.get(planId);
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    return plan.activityInstances
        .entrySet()
        .stream()
        .map(entry -> Pair.of(entry.getKey(), new ActivityInstance(entry.getValue())));
  }

  @Override
  public ActivityInstance getActivityInPlanById(final String planId, final String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final Plan plan = Optional
        .ofNullable(this.plans.get(planId))
        .orElseThrow(() -> new NoSuchPlanException(planId));

    final ActivityInstance activityInstance = Optional
        .ofNullable(plan.activityInstances.get(activityId))
        .orElseThrow(() -> new NoSuchActivityInstanceException(planId, activityId));

    return new ActivityInstance(activityInstance);
  }

  @Override
  public String createPlan(final NewPlan newPlan) {
    final String planId = Objects.toString(this.nextPlanId++);

    final Plan plan = new Plan();
    plan.name = newPlan.name;
    plan.startTimestamp = newPlan.startTimestamp;
    plan.endTimestamp = newPlan.endTimestamp;
    plan.adaptationId = newPlan.adaptationId;
    plan.activityInstances = new HashMap<>();

    if (newPlan.activityInstances != null) {
      for (final var activity : newPlan.activityInstances) {
        final String activityId = Objects.toString(this.nextActivityId++);
        plan.activityInstances.put(activityId, new ActivityInstance(activity));
      }
    }

    this.plans.put(planId, plan);
    return planId;
  }

  @Override
  public PlanTransaction updatePlan(final String id) {
    return new MockPlanTransaction(id);
  }

  @Override
  public void replacePlan(final String id, final NewPlan newPlan) throws NoSuchPlanException {
    if (!this.plans.containsKey(id)) {
      throw new NoSuchPlanException(id);
    }

    final Plan plan = new Plan();
    plan.name = newPlan.name;
    plan.startTimestamp = newPlan.startTimestamp;
    plan.endTimestamp = newPlan.endTimestamp;
    plan.adaptationId = newPlan.adaptationId;
    plan.activityInstances = new HashMap<>();

    if (newPlan.activityInstances != null) {
      for (final var activity : newPlan.activityInstances) {
        final String activityId = Objects.toString(this.nextActivityId++);
        plan.activityInstances.put(activityId, new ActivityInstance(activity));
      }
    }

    this.plans.put(id, plan);
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
    final Plan plan = this.plans.get(planId);
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    final String activityId = Objects.toString(this.nextActivityId++);
    plan.activityInstances.put(activityId, new ActivityInstance(activity));
    return activityId;
  }

  @Override
  public ActivityTransaction updateActivity(final String planId, final String activityId) {
    return new MockActivityTransaction(planId, activityId);
  }

  @Override
  public void replaceActivity(final String planId, final String activityId, final ActivityInstance activity) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final Plan plan = this.plans.get(planId);
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    if (!plan.activityInstances.containsKey(activityId)) {
      throw new NoSuchActivityInstanceException(planId, activityId);
    }

    plan.activityInstances.put(activityId, activity);
  }

  @Override
  public void deleteActivity(final String planId, final String activityId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    final Plan plan = this.plans.get(planId);
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    if (!plan.activityInstances.containsKey(activityId)) {
      throw new NoSuchActivityInstanceException(planId, activityId);
    }

    plan.activityInstances.remove(activityId);
  }

  @Override
  public void deleteAllActivities(final String planId) throws NoSuchPlanException {
    final Plan plan = plans.get(planId);
    if (plan == null) {
      throw new NoSuchPlanException(planId);
    }

    plan.activityInstances.clear();
  }

  private class MockPlanTransaction implements PlanTransaction {
    private final String planId;

    private Optional<String> name = Optional.empty();
    private Optional<String> startTimestamp = Optional.empty();
    private Optional<String> endTimestamp = Optional.empty();
    private Optional<String> adaptationId = Optional.empty();

    public MockPlanTransaction(final String planId) {
      this.planId = planId;
    }

    @Override
    public void commit() throws NoSuchPlanException {
      final Plan plan = MockPlanRepository.this.plans.get(this.planId);
      if (plan == null) {
        throw new NoSuchPlanException(this.planId);
      }

      this.name.ifPresent(name -> plan.name = name);
      this.startTimestamp.ifPresent(startTimestamp -> plan.startTimestamp = startTimestamp);
      this.endTimestamp.ifPresent(endTimestamp -> plan.endTimestamp = endTimestamp);
      this.adaptationId.ifPresent(adaptationId -> plan.adaptationId = adaptationId);
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.name = Optional.of(name);
      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final String timestamp) {
      this.startTimestamp = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final String timestamp) {
      this.endTimestamp = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setAdaptationId(final String adaptationId) {
      this.adaptationId = Optional.of(adaptationId);
      return this;
    }
  }

  private class MockActivityTransaction implements ActivityTransaction {
    private final String planId;
    private final String activityId;

    private Optional<String> type = Optional.empty();
    private Optional<String> startTimestamp = Optional.empty();
    private Optional<Map<String, SerializedValue>> parameters = Optional.empty();

    public MockActivityTransaction(final String planId, final String activityId) {
      this.planId = planId;
      this.activityId = activityId;
    }

    @Override
    public void commit() throws NoSuchPlanException, NoSuchActivityInstanceException {
      final Plan plan = MockPlanRepository.this.plans.get(this.planId);
      if (plan == null) {
        throw new NoSuchPlanException(this.planId);
      }

      final ActivityInstance activity = plan.activityInstances.get(this.activityId);
      if (activity == null) {
        throw new NoSuchActivityInstanceException(this.planId, this.activityId);
      }

      this.type.ifPresent(type -> activity.type = type);
      this.startTimestamp.ifPresent(startTimestamp -> activity.startTimestamp = startTimestamp);
      this.parameters.ifPresent(parameters -> activity.parameters = parameters);
    }

    @Override
    public ActivityTransaction setType(final String type) {
      this.type = Optional.of(type);
      return this;
    }

    @Override
    public ActivityTransaction setStartTimestamp(final String timestamp) {
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
