package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.ParameterSchema;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.ValidationException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository.PlanTransaction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public final class LocalPlanService implements PlanService {
  private final PlanRepository planRepository;
  private final AdaptationService adaptationService;
  private final Consumer<Map<String, Pair<ValueSchema, SerializedValue>>> parameterUpdateListener;

  public LocalPlanService(
      final PlanRepository planRepository,
      final AdaptationService adaptationService,
      final Consumer<Map<String, Pair<ValueSchema, SerializedValue>>> parameterUpdateListener
  ) {
    this.planRepository = planRepository;
    this.adaptationService = adaptationService;
    this.parameterUpdateListener = parameterUpdateListener;
  }

  public LocalPlanService(
      final PlanRepository planRepository,
      final AdaptationService adaptationService
  ) {
    this(planRepository, adaptationService, $ -> { });
  }

  @Override
  public Stream<Pair<String, Plan>> getPlans() {
    return this.planRepository.getAllPlans();
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
  public String addPlan(final NewPlan plan) throws ValidationException {
    withValidator(validator -> validator.validateNewPlan(plan));
    return this.planRepository.createPlan(plan);
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    this.planRepository.deletePlan(id);
  }

  @Override
  public void updatePlan(final String planId, final Plan patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validatePlanPatch(adaptationId, planId, patch));

    final PlanTransaction transaction = this.planRepository.updatePlan(planId);
    if (patch.name != null) transaction.setName(patch.name);
    if (patch.startTimestamp != null) transaction.setStartTimestamp(patch.startTimestamp);
    if (patch.endTimestamp != null) transaction.setEndTimestamp(patch.endTimestamp);
    transaction.commit();

    if (patch.activityInstances != null) {
      for (final var entry : patch.activityInstances.entrySet()) {
        if (entry.getValue() == null) {
          this.removeActivityInstanceById(planId, entry.getKey());
        } else {
          this.replaceActivityInstance(planId, entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    withValidator(validator -> validator.validateNewPlan(plan));
    this.planRepository.replacePlan(id, plan);
  }

  @Override
  public ActivityInstance getActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    return this.planRepository.getActivityInPlanById(planId, activityInstanceId);
  }

  @Override
  public List<String> addActivityInstancesToPlan(final String planId, final List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validateActivityList(adaptationId, activityInstances));

    final List<String> activityInstanceIds = new ArrayList<>(activityInstances.size());
    for (final ActivityInstance activityInstance : activityInstances) {
      final String activityInstanceId = this.planRepository.createActivity(planId, activityInstance);
      activityInstanceIds.add(activityInstanceId);
    }

    return activityInstanceIds;
  }

  @Override
  public void removeActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    this.planRepository.deleteActivity(planId, activityInstanceId);
  }

  @Override
  public void updateActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance patch) throws NoSuchPlanException, NoSuchActivityInstanceException, ValidationException {
    final Plan plan = this.planRepository.getPlan(planId);

    final ActivityInstance activityInstance = Optional
        .ofNullable(plan.activityInstances.getOrDefault(activityInstanceId, null))
        .orElseThrow(() -> new NoSuchActivityInstanceException(planId, activityInstanceId));

    if (patch.type != null) activityInstance.type = patch.type;
    if (patch.startTimestamp != null) activityInstance.startTimestamp = patch.startTimestamp;
    if (patch.parameters != null) activityInstance.parameters = patch.parameters;

    withValidator(validator -> validator.validateActivity(plan.adaptationId, activityInstance));

    interceptActivityInstance(plan.adaptationId, activityInstanceId, activityInstance);
    this.planRepository.replaceActivity(planId, activityInstanceId, activityInstance);
  }

  @Override
  public void replaceActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance activityInstance) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validateActivity(adaptationId, activityInstance));

    interceptActivityInstance(adaptationId, activityInstanceId, activityInstance);
    this.planRepository.replaceActivity(planId, activityInstanceId, activityInstance);
  }

  @Override
  public Map<String, Constraint> getConstraintsForPlan(final String planId) throws NoSuchPlanException {
    return this.planRepository.getAllConstraintsInPlan(planId);
  }

  @Override
  public void replaceConstraints(final String planId, final Map<String, Constraint> constraints) throws NoSuchPlanException {
    this.planRepository.replacePlanConstraints(planId, constraints);
  }

  @Override
  public void removeConstraintById(final String planId, final String constraintId)
  throws NoSuchPlanException
  {
    this.planRepository.deleteConstraintInPlanById(planId, constraintId);
  }

  private <T extends Throwable> void withValidator(final ValidationScope<T> block) throws ValidationException, T {
    final var validator = new PlanValidator(this.planRepository, this.adaptationService);

    block.accept(validator);
    final var messages = validator.getMessages();

    if (messages.size() > 0) throw new ValidationException(messages);
  }

  private void interceptActivityInstance(final String adaptationId, final String activityInstanceId, final ActivityInstance activityInstance)
  {
    try {
      final var schemas = this.adaptationService.getActivityParameterSchemas(adaptationId, activityInstanceId);
      final var parameterSchemaValues = schemas.stream().collect(
          toMap(schema -> schema.name,
                schema -> Pair.of(schema.schema, activityInstance.parameters.get(schema.name))));
      parameterUpdateListener.accept(parameterSchemaValues);
    } catch (final AdaptationService.NoSuchAdaptationException e) {
      throw new Error("Unexpectedly nonexistent adaptation, when this should have been loaded earlier.", e);
    } catch (final AdaptationService.NoSuchActivityTypeException e) {
      throw new Error("Unexpectedly nonexistent activity type.", e);
    }
  }

  @FunctionalInterface
  private interface ValidationScope<T extends Throwable> {
    void accept(PlanValidator validator) throws T;
  }
}
