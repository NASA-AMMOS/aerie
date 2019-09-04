package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class MockPlanRepository implements PlanRepository {
  private final Map<String, Plan> plans = new HashMap<>();
  private int nextPlanId = 0;
  private int nextActivityId = 0;

  @Override
  public PlanTransaction newPlan() {
    return new MockPlanTransaction();
  }

  @Override
  public Optional<PlanTransaction> getPlan(final String id) {
    return Optional
        .ofNullable(this.plans.get(id))
        .map(plan -> new MockPlanTransaction(id, new Plan(plan)));
  }

  @Override
  public Stream<PlanTransaction> getAllPlans() {
    // Fetch all keys up front so we don't incur a ConcurrentModificationException
    // if an entry is deleted during iteration.
    final List<String> keys = new ArrayList<>(this.plans.keySet());

    return keys
        .stream()
        .map(key -> Optional
            .ofNullable(this.plans.get(key))
            .map(plan -> Pair.of(key, plan)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(MockPlanTransaction::new);
  }

  private class MockPlanTransaction implements PlanTransaction {
    private final Plan plan;
    private String id;

    public MockPlanTransaction() {
      this.id = null;
      this.plan = new Plan();
      this.plan.activityInstances = new HashMap<>();
    }

    public MockPlanTransaction(final Pair<String, Plan> entry) {
      this.id = entry.getKey();
      this.plan = entry.getValue();
    }

    public MockPlanTransaction(final String id, final Plan plan) {
      this.id = id;
      this.plan = plan;
    }

    @Override
    public String getId() {
      return this.id;
    }

    @Override
    public Plan get() {
      return new Plan(this.plan);
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.plan.name = name;
      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final String timestamp) {
      this.plan.startTimestamp = timestamp;
      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final String timestamp) {
      this.plan.endTimestamp = timestamp;
      return this;
    }

    @Override
    public PlanTransaction setAdaptationId(final String adaptationId) {
      this.plan.adaptationId = adaptationId;
      return this;
    }

    @Override
    public ActivityTransaction newActivity() {
      return new MockActivityTransaction(this.plan.activityInstances);
    }

    @Override
    public Optional<ActivityTransaction> getActivity(final String activityId) {
      return Optional
          .ofNullable(this.plan.activityInstances.get(activityId))
          .map(ActivityInstance::new)
          .map(activity -> new MockActivityTransaction(this.plan.activityInstances, activityId, activity));
    }

    @Override
    public Stream<ActivityTransaction> getAllActivities() {
      // Fetch all keys up front so we don't incur a ConcurrentModificationException
      // if an entry is deleted during iteration.
      final List<String> keys = new ArrayList<>(this.plan.activityInstances.keySet());

      return keys
          .stream()
          .map(key -> Optional
              .ofNullable(this.plan.activityInstances.get(key))
              .map(plan -> Pair.of(key, plan)))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(entry -> new MockActivityTransaction(this.plan.activityInstances, entry));
    }

    @Override
    public String save() {
      if (this.id == null) {
        this.id = Objects.toString(MockPlanRepository.this.nextPlanId++);
      }

      MockPlanRepository.this.plans.put(this.id, new Plan(this.plan));
      return this.id;
    }

    @Override
    public void delete() {
      if (this.id == null) {
        return;
      }

      MockPlanRepository.this.plans.remove(this.id);
      this.id = null;
    }
  }

  private class MockActivityTransaction implements ActivityTransaction {
    private final Map<String, ActivityInstance> collection;
    private final ActivityInstance activityInstance;
    private String id;

    public MockActivityTransaction(final Map<String, ActivityInstance> collection) {
      this.collection = collection;
      this.id = null;
      this.activityInstance = new ActivityInstance();
    }

    public MockActivityTransaction(final Map<String, ActivityInstance> collection, final Pair<String, ActivityInstance> entry) {
      this.collection = collection;
      this.id = entry.getKey();
      this.activityInstance = entry.getValue();
    }

    public MockActivityTransaction(final Map<String, ActivityInstance> collection, final String id, final ActivityInstance activityInstance) {
      this.collection = collection;
      this.id = id;
      this.activityInstance = activityInstance;
    }

    @Override
    public String getId() {
      return this.id;
    }

    @Override
    public ActivityInstance get() {
      return new ActivityInstance(activityInstance);
    }

    @Override
    public ActivityTransaction setType(final String type) {
      this.activityInstance.type = type;
      return this;
    }

    @Override
    public ActivityTransaction setStartTimestamp(final String timestamp) {
      this.activityInstance.startTimestamp = timestamp;
      return this;
    }

    @Override
    public ActivityTransaction setParameters(final Map<String, ActivityParameter> parameters) {
      if (parameters == null) {
        this.activityInstance.parameters = null;
      } else {
        this.activityInstance.parameters = new HashMap<>();
        for (final var entry : parameters.entrySet()) {
          this.activityInstance.parameters.put(entry.getKey(), new ActivityParameter(entry.getValue()));
        }
      }

      return this;
    }

    @Override
    public String save() {
      if (this.id == null) {
        this.id = Objects.toString(MockPlanRepository.this.nextActivityId++);
      }

      this.collection.put(this.id, new ActivityInstance(this.activityInstance));
      return this.id;
    }

    @Override
    public void delete() {
      if (this.id == null) {
        return;
      }

      this.collection.remove(this.id);
      this.id = null;
    }
  }
}
