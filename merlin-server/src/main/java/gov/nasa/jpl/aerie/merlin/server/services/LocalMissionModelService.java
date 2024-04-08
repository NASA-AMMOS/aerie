package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelId;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/**
 * Implements the missionModel service {@link MissionModelService} interface on a set of local domain objects.
 *
 * May throw unchecked exceptions:
 * * {@link MissionModelLoadException}: When a mission model cannot be loaded from the JAR provided by the
 * connected mission model repository.
 */
public final class LocalMissionModelService implements MissionModelService {
  private static final Logger log = LoggerFactory.getLogger(LocalMissionModelService.class);

  private final Path missionModelDataPath;
  private final MissionModelRepository missionModelRepository;
  private final Instant untruePlanStart;

  private boolean doingIncrementalSim = true;

  private final Map<Triple<String, Instant, Duration>, SimulationDriver>
      simulationDrivers = new HashMap<Triple<String, Instant, Duration>, SimulationDriver>();

  public LocalMissionModelService(
      final Path missionModelDataPath,
      final MissionModelRepository missionModelRepository,
      final Instant untruePlanStart
  ) {
    this.missionModelDataPath = missionModelDataPath;
    this.missionModelRepository = missionModelRepository;
    this.untruePlanStart = untruePlanStart;
  }

  @Override
  public Map<String, MissionModelJar> getMissionModels() {
    return this.missionModelRepository.getAllMissionModels();
  }

  @Override
  public MissionModelJar getMissionModelById(final String id) throws NoSuchMissionModelException {
    try {
      return this.missionModelRepository.getMissionModel(id);
    } catch (MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(id, ex);
    }
  }

  @Override
  public Map<Long, Constraint> getConstraints(final String missionModelId) throws NoSuchMissionModelException {
    try {
      return this.missionModelRepository.getConstraints(missionModelId);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public Map<String, ValueSchema> getResourceSchemas(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    // TODO: [AERIE-1516] Teardown the missionModel after use to release any system resources (e.g. threads).
    final var schemas = new HashMap<String, ValueSchema>();

    for (final var entry : loadAndInstantiateMissionModel(missionModelId).getResources().entrySet()) {
      final var name = entry.getKey();
      final var resource = entry.getValue();
      schemas.put(name, resource.getOutputType().getSchema());
    }

    return schemas;
  }

  /**
   * Get information about all activity types in the named mission model.
   *
   * @param missionModelId The ID of the mission model to load.
   * @return The set of all activity types in the named mission model, indexed by name.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public Map<String, ActivityType> getActivityTypes(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    try {
      return missionModelRepository.getActivityTypes(missionModelId);
    } catch (MissionModelRepository.NoSuchMissionModelException e) {
      throw new NoSuchMissionModelException(missionModelId, e);
    }
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param missionModelId The ID of the mission model to load.
   * @param activity The serialized activity to validate against the named mission model.
   * @return A list of validation errors that is empty if validation succeeds.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public List<ValidationNotice> validateActivityArguments(final String missionModelId, final SerializedActivity activity)
  throws NoSuchMissionModelException, MissionModelLoadException, InstantiationException
  {
    // TODO: [AERIE-1516] Teardown the missionModel after use to release any system resources (e.g. threads).
    final var modelType = this.loadMissionModelType(missionModelId);
    final var registry = DirectiveTypeRegistry.extract(modelType);
    final var directiveType = registry.directiveTypes().get(activity.getTypeName());
    if (directiveType == null) return List.of(new ValidationNotice(List.of(), "unknown activity type"));
    return directiveType.getInputType().validateArguments(activity.getArguments());
  }

  public List<BulkArgumentValidationResponse> validateActivityArgumentsBulk(
      final MissionModelId modelId,
      final List<ActivityDirectiveForValidation> activities
  ) throws NoSuchMissionModelException, MissionModelLoadException {
    // load mission model once for all activities
    final var modelType = this.loadMissionModelType(modelId.toString());
    final var registry = DirectiveTypeRegistry.extract(modelType);

    // map all directives to validation response
    return activities.stream().map((directive) -> {
      final var typeName = directive.activity().getTypeName();
      final var arguments = directive.activity().getArguments();

      try {
        final var directiveType = registry.directiveTypes().get(typeName);
        if (directiveType == null) {
          return new BulkArgumentValidationResponse.NoSuchActivityError(new NoSuchActivityTypeException(typeName));
        }

        final var notices = directiveType.getInputType().validateArguments(arguments);
        return notices.isEmpty()
            ? new BulkArgumentValidationResponse.Success()
            : new BulkArgumentValidationResponse.Validation(notices);
      } catch (InstantiationException e) {
        return new BulkArgumentValidationResponse.InstantiationError(e);
      }
    }).collect(Collectors.toList());
  }

  public Map<MissionModelId, List<ActivityDirectiveForValidation>> getUnvalidatedDirectives() {
    return missionModelRepository.getUnvalidatedDirectives();
  }

  public void updateDirectiveValidations(List<Pair<ActivityDirectiveForValidation, BulkArgumentValidationResponse>> updates) {
    missionModelRepository.updateDirectiveValidations(updates);
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param missionModelId The ID of the mission model to load.
   * @param activities The serialized activities to perform instantiation validation against the named mission model.
   * @return A map of validation errors mapping activity instance ID to failure message. If validation succeeds the map is empty.
   */
  @Override
  public Map<ActivityDirectiveId, ActivityInstantiationFailure>
  validateActivityInstantiations(final String missionModelId,
                                 final Map<ActivityDirectiveId, SerializedActivity> activities)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    final var factory = this.loadMissionModelType(missionModelId);
    final var registry = DirectiveTypeRegistry.extract(factory);

    final var failures = new HashMap<ActivityDirectiveId, ActivityInstantiationFailure>();

    for (final var entry : activities.entrySet()) {
      final var id = entry.getKey();
      final var act = entry.getValue();
      try {
        // The return value is intentionally ignored - we are only interested in failures
        final var specType = Optional
        .ofNullable(registry.directiveTypes().get(act.getTypeName()))
        .orElseThrow(() -> new MissionModelService.NoSuchActivityTypeException(act.getTypeName()));
        specType.getInputType().getEffectiveArguments(act.getArguments());
      } catch (final NoSuchActivityTypeException ex) {
        failures.put(id, new ActivityInstantiationFailure.NoSuchActivityType(ex));
      } catch (final InstantiationException ex) {
        failures.put(id, new ActivityInstantiationFailure.InstantiationFailure(ex));
      }
    }

    return failures;
  }

  @Override
  public List<BulkEffectiveArgumentResponse> getActivityEffectiveArgumentsBulk(
      final String missionModelId,
      final List<SerializedActivity> serializedActivities)
  throws NoSuchMissionModelException, MissionModelLoadException {
      final var modelType = this.loadMissionModelType(missionModelId);
      final var registry = DirectiveTypeRegistry.extract(modelType);
      final var response = new ArrayList<BulkEffectiveArgumentResponse>();

      for (final var activity : serializedActivities) {
        final var typeName = activity.getTypeName();

        try {
          final var directiveType = Optional
              .ofNullable(registry.directiveTypes().get(typeName))
              .orElseThrow(() -> new NoSuchActivityTypeException(activity.getTypeName()));

          response.add(new BulkEffectiveArgumentResponse.Success(
              new SerializedActivity(
              typeName,
              directiveType.getInputType().getEffectiveArguments(activity.getArguments())
          )));
        } catch (NoSuchActivityTypeException e) {
          response.add(new BulkEffectiveArgumentResponse.TypeFailure(e));
        } catch (InstantiationException e) {
          response.add(new BulkEffectiveArgumentResponse.InstantiationFailure(e));
        }
      }

      return response;
  }

  @Override
  public List<ValidationNotice> validateModelArguments(final String missionModelId, final Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         MissionModelLoadException,
         InstantiationException
  {
    return this.loadMissionModelType(missionModelId)
        .getConfigurationType()
        .validateArguments(arguments);
  }

  @Override
  public List<Parameter> getModelParameters(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return this.loadMissionModelType(missionModelId).getConfigurationType().getParameters();
  }

  @Override
  public Map<String, SerializedValue> getModelEffectiveArguments(final String missionModelId, final Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         MissionModelLoadException,
         InstantiationException
  {
    return this.loadMissionModelType(missionModelId)
        .getConfigurationType()
        .getEffectiveArguments(arguments);
  }

  protected static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param message The parameters defining the simulation to perform.
   * @return A set of samples over the course of the simulation.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  @Override
  public SimulationResultsInterface runSimulation(
      final CreateSimulationMessage message,
      final Consumer<Duration> simulationExtentConsumer,
      final Supplier<Boolean> canceledListener)
  throws NoSuchMissionModelException
  {
    long accumulatedCpuTime = 0;  // nanoseconds
    long initialCpuTime = threadMXBean.getCurrentThreadCpuTime();  // nanoseconds
    final var config = message.configuration();
    if (config.isEmpty()) {
      log.warn(
          "No mission model configuration defined for mission model. Simulations will receive an empty set of configuration arguments.");
    }

    var planInfo = Triple.of(message.missionModelId(), message.planStartTime(), message.planDuration());
    SimulationDriver<?> driver = simulationDrivers.get(planInfo);

    SimulationResultsInterface results;
    if (driver == null || !doingIncrementalSim) {
      final MissionModel<?> missionModel = loadAndInstantiateMissionModel(message.missionModelId(),
                                                                          message.simulationStartTime(),
                                                                          SerializedValue.of(config));

      driver = new SimulationDriver<>(missionModel, message.planStartTime(), message.planDuration(),
                                      message.useResourceTracker());
      simulationDrivers.put(planInfo, driver);
      // TODO: [AERIE-1516] Teardown the mission model after use to release any system resources (e.g. threads).
      results = driver.simulate(
          message.activityDirectives(),
          message.simulationStartTime(),
          message.simulationDuration(),
          message.planStartTime(),
          message.planDuration(),
          true,
          canceledListener,
          simulationExtentConsumer);
    } else {
      // Try to reuse past simulation.
      driver.initSimulation(message.simulationDuration());
      results = driver.diffAndSimulate(message.activityDirectives(),
                                    message.simulationStartTime(),
                                    message.simulationDuration(),
                                    message.planStartTime(),
                                    message.planDuration(),
                                    true,
                                    canceledListener,
                                    simulationExtentConsumer);
    }
    accumulatedCpuTime = threadMXBean.getCurrentThreadCpuTime() - initialCpuTime;
    System.out.println("LocalMissionModelService.runSimulation() CPU time: " + formatTimestamp(accumulatedCpuTime));
    return results;
  }

  /**
   * ISO timestamp format
   */
  public static final DateTimeFormatter format =
      new DateTimeFormatterBuilder()
          .appendPattern("uuuu-DDD'T'HH:mm:ss")
          .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
          .toFormatter();

  /**
   * Format Instant into a date-timestamp.
   *
   * @param instant
   * @return formatted string
   */  protected static String formatTimestamp(Instant instant) {
    return format.format(instant.atZone(ZoneOffset.UTC));
  }

  /**
   * Format nanoseconds into a date-timestamp.
   *
   * @param nanoseconds since the Java epoch, Jan 1, 1970
   * @return formatted string
   */
  protected static String formatTimestamp(long nanoseconds) {
    System.nanoTime();
    return formatTimestamp(Instant.ofEpochSecond(0L, nanoseconds));
  }
  @Override
  public void refreshModelParameters(final String missionModelId)
  throws NoSuchMissionModelException
  {
    try {
      this.missionModelRepository.updateModelParameters(missionModelId, getModelParameters(missionModelId));
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public void refreshActivityTypes(final String missionModelId)
  throws NoSuchMissionModelException
  {
    try {
      final var modelType = this.loadMissionModelType(missionModelId);
      final var registry = DirectiveTypeRegistry.extract(modelType);
      final var activityTypes = new HashMap<String, ActivityType>();
      registry.directiveTypes().forEach((name, directiveType) -> {
        final var inputType = directiveType.getInputType();
        final var outputType = directiveType.getOutputType();
        activityTypes.put(name, new ActivityType(
            name,
            inputType.getParameters(),
            inputType.getRequiredParameters(),
            outputType.getSchema()));
      });
      this.missionModelRepository.updateActivityTypes(missionModelId, activityTypes);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public void refreshResourceTypes(final String missionModelId)
  throws NoSuchMissionModelException {
    try {
      final var model = this.loadAndInstantiateMissionModel(missionModelId);
      this.missionModelRepository.updateResourceTypes(missionModelId, model.getResources());
    } catch (MissionModelRepository.NoSuchMissionModelException e) {
      throw new NoSuchMissionModelException(missionModelId);
    }
  }

  private ModelType<?, ?> loadMissionModelType(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    try {
      final var missionModelJar = this.missionModelRepository.getMissionModel(missionModelId);
      return MissionModelLoader.loadModelType(missionModelDataPath.resolve(missionModelJar.path), missionModelJar.name, missionModelJar.version);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    } catch (final MissionModelLoader.MissionModelLoadException ex) {
      throw new MissionModelLoadException(ex);
    }
  }

  /**
   * Load a {@link MissionModel} from the mission model repository using the mission model's default mission model configuration
   *
   * @param missionModelId The ID of the mission model in the mission model repository to load.
   * @return A {@link MissionModel} domain object allowing use of the loaded mission model.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  private MissionModel<?> loadAndInstantiateMissionModel(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return loadAndInstantiateMissionModel(missionModelId, untruePlanStart, SerializedValue.of(Map.of()));
  }

  /**
   * Load a {@link MissionModel} from the mission model repository.
   *
   * @param missionModelId The ID of the mission model in the mission model repository to load.
   * @param configuration The mission model configuration to load the mission model with.
   * @return A {@link MissionModel} domain object allowing use of the loaded mission model.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  private MissionModel<?> loadAndInstantiateMissionModel(
      final String missionModelId,
      final Instant planStart,
      final SerializedValue configuration)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    try {
      final var missionModelJar = this.missionModelRepository.getMissionModel(missionModelId);
      return MissionModelLoader.loadMissionModel(
          planStart,
          configuration,
          missionModelDataPath.resolve(missionModelJar.path),
          missionModelJar.name,
          missionModelJar.version);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    } catch (final MissionModelLoader.MissionModelLoadException ex) {
      throw new MissionModelLoadException(ex);
    }
  }

  public static class MissionModelLoadException extends RuntimeException {
    public MissionModelLoadException(final Throwable cause) { super(cause); }
  }
}
