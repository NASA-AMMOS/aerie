package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import gov.nasa.jpl.aerie.types.MissionModelId;
import gov.nasa.jpl.aerie.types.Plan;
import gov.nasa.jpl.aerie.types.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser;
import gov.nasa.jpl.aerie.merlin.driver.resources.SimulationResourceManager;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
  public Map<MissionModelId, MissionModelJar> getMissionModels() {
    return this.missionModelRepository.getAllMissionModels();
  }

  @Override
  public MissionModelJar getMissionModelById(final MissionModelId missionModelId) throws NoSuchMissionModelException {
    try {
      return this.missionModelRepository.getMissionModel(missionModelId);
    } catch (MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public Map<String, ValueSchema> getResourceSchemas(final MissionModelId missionModelId)
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
   */
  @Override
  public Map<String, ActivityType> getActivityTypes(final MissionModelId missionModelId)
  throws NoSuchMissionModelException
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
  public List<ValidationNotice> validateActivityArguments(final MissionModelId missionModelId, final SerializedActivity activity)
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
      final MissionModelId missionModelId,
      final List<ActivityDirectiveForValidation> activities) {
    // load mission model once for all activities
    ModelType<?, ?> modelType;
    try {
      modelType = this.loadMissionModelType(missionModelId);
      // try and catch NoSuchMissionModel here, so we can serialize it out to each activity validation
      // rather than catching it at a higher level in the workerLoop itself
    } catch (NoSuchMissionModelException e) {
      return activities.stream()
          .map(directive -> new BulkArgumentValidationResponse.NoSuchMissionModelError(e))
          .collect(Collectors.toList());
    } catch (MissionModelLoadException e) {
      log.error("Caught MissionModelLoadException, skipping this batch but leaving validations pending...");
      log.error(e.toString());
      return List.of();
    }
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
  validateActivityInstantiations(final MissionModelId missionModelId,
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
      final MissionModelId missionModelId,
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
  public List<ValidationNotice> validateModelArguments(final MissionModelId missionModelId, final Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         MissionModelLoadException,
         InstantiationException
  {
    return this.loadMissionModelType(missionModelId)
        .getConfigurationType()
        .validateArguments(arguments);
  }

  @Override
  public List<Parameter> getModelParameters(final MissionModelId missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return this.loadMissionModelType(missionModelId).getConfigurationType().getParameters();
  }

  @Override
  public Map<String, SerializedValue> getModelEffectiveArguments(final MissionModelId missionModelId, final Map<String, SerializedValue> arguments)
  throws NoSuchMissionModelException,
         MissionModelLoadException,
         InstantiationException
  {
    return this.loadMissionModelType(missionModelId)
        .getConfigurationType()
        .getEffectiveArguments(arguments);
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param plan The plan to be simulated. Contains the parameters defining the simulation to perform.
   * @return A set of samples over the course of the simulation.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  @Override
  public Pair<SimulationResults, SerializedValue> runSimulation(
      final Plan plan,
      final Consumer<Duration> simulationExtentConsumer,
      final Supplier<Boolean> canceledListener,
      final SimulationResourceManager resourceManager)
  throws NoSuchMissionModelException
  {
    if (plan.initialConditions instanceof Plan.InitialConditions.FromArguments c && c.configuration().isEmpty()) {
      log.warn(
          "No mission model configuration defined for mission model. Simulations will receive an empty set of configuration arguments.");
    }

//    final String inconsFile = "fincons.json";
    final Optional<SerializedValue> incons;
//    if (new File(inconsFile).exists()) {
//      incons = Optional.of(readJsonFromFile(inconsFile));
//    } else {
//      incons = Optional.empty();
//    }

    if (plan.initialConditions instanceof Plan.InitialConditions.FromFincons c) {
      incons = Optional.of(c.fincons());
    } else {
      incons = Optional.empty();
    }

    // TODO: [AERIE-1516] Teardown the mission model after use to release any system resources (e.g. threads).
    return SimulationDriver.simulate(
        loadAndInstantiateMissionModel(
            plan.missionModelId(),
            plan.planStartInstant(),
            plan.initialConditions instanceof Plan.InitialConditions.FromArguments c ? SerializedValue.of(c.configuration()) : SerializedValue.of(Map.of())),
        plan.activityDirectives(),
        plan.simulationStartInstant(),
        plan.simulationDuration(),
        plan.planStartInstant(),
        plan.duration(),
        canceledListener,
        simulationExtentConsumer,
        resourceManager,
        incons);

//    final var simulationResults = response.getLeft();
//    final var fincons = response.getRight();
//
//    try {
//      Files.write(Path.of(inconsFile), List.of(new SerializedValueJsonParser().unparse(fincons).toString()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//
//    return simulationResults;
  }

  private static SerializedValue readJsonFromFile(String inconsFile) {
    try {
      return new SerializedValueJsonParser().parse(
          Json
              .createReader(new StringReader(Files.readString(Path.of(inconsFile))))
              .readValue()).getSuccessOrThrow();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void refreshModelParameters(final MissionModelId missionModelId)
  throws NoSuchMissionModelException
  {
    try {
      this.missionModelRepository.updateModelParameters(missionModelId, getModelParameters(missionModelId));
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public void refreshActivityTypes(final MissionModelId missionModelId)
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
  public void refreshResourceTypes(final MissionModelId missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException {
    try {
      final var model = this.loadAndInstantiateMissionModel(missionModelId);
      this.missionModelRepository.updateResourceTypes(missionModelId, model.getResources());
    } catch (MissionModelRepository.NoSuchMissionModelException e) {
      throw new NoSuchMissionModelException(missionModelId);
    }
  }

  private ModelType<?, ?> loadMissionModelType(final MissionModelId missionModelId)
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
  private MissionModel<?> loadAndInstantiateMissionModel(final MissionModelId missionModelId)
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
      final MissionModelId missionModelId,
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
