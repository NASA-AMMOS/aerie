package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implements the missionModel service {@link MissionModelService} interface on a set of local domain objects.
 *
 * May throw unchecked exceptions:
 * * {@link MissionModelLoadException}: When a mission model cannot be loaded from the JAR provided by the
 * connected mission model repository.
 */
public final class LocalMissionModelService implements MissionModelService {
  private static final Logger log = Logger.getLogger(LocalMissionModelService.class.getName());

  private final Path missionModelDataPath;
  private final MissionModelRepository missionModelRepository;

  public LocalMissionModelService(
      final Path missionModelDataPath,
      final MissionModelRepository missionModelRepository
  ) {
    this.missionModelDataPath = missionModelDataPath;
    this.missionModelRepository = missionModelRepository;
  }

  @Override
  public Map<String, MissionModelJar> getMissionModels() {
    return this.missionModelRepository.getAllMissionModels();
  }

  @Override
  public MissionModelJar getMissionModelById(String id) throws NoSuchMissionModelException {
    try {
      return this.missionModelRepository.getMissionModel(id);
    } catch (MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(id, ex);
    }
  }

  @Override
  public Map<String, Constraint> getConstraints(final String missionModelId) throws NoSuchMissionModelException {
    try {
      return this.missionModelRepository.getConstraints(missionModelId);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  @Override
  public Map<String, ValueSchema> getStatesSchemas(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    // TODO: [AERIE-1516] Teardown the missionModel after use to release any system resources (e.g. threads).
    return loadConfiguredMissionModel(missionModelId).getStateSchemas();
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
  public Map<String, ActivityType> getActivityTypes(String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return loadUnconfiguredMissionModel(missionModelId)
        .getActivityTypes();
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param missionModelId The ID of the mission model to load.
   * @param activityParameters The serialized activity to validate against the named mission model.
   * @return A list of validation errors that is empty if validation succeeds.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   * @throws MissionModelFacade.MissionModelContractException If the named mission model does not abide by the expected contract.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public List<String> validateActivityParameters(final String missionModelId, final SerializedActivity activityParameters)
  throws NoSuchMissionModelException, MissionModelFacade.MissionModelContractException, MissionModelLoadException
  {
    try {
      // TODO: [AERIE-1516] Teardown the missionModel after use to release any system resources (e.g. threads).
      return this.loadConfiguredMissionModel(missionModelId)
                 .validateActivity(activityParameters.getTypeName(), activityParameters.getArguments());
    } catch (final MissionModelFacade.NoSuchActivityTypeException ex) {
      return List.of("unknown activity type");
    } catch (final MissionModelFacade.UnconstructableActivityInstanceException ex) {
      return List.of(ex.getMessage());
    }
  }

  @Override
  public Map<String, SerializedValue> getActivityEffectiveArguments(final String missionModelId, final SerializedActivity activity)
  throws NoSuchMissionModelException,
         NoSuchActivityTypeException,
         UnconstructableActivityInstanceException,
         MissingArgumentException,
         MissionModelLoadException
  {
    try {
      return this.loadConfiguredMissionModel(missionModelId)
                 .getActivityEffectiveArguments(activity.getTypeName(), activity.getArguments());
    } catch (final MissionModelFacade.NoSuchActivityTypeException ex) {
      throw new NoSuchActivityTypeException(activity.getTypeName(), ex);
    } catch (final MissionModelFacade.UnconstructableActivityInstanceException ex) {
      throw new UnconstructableActivityInstanceException(activity.getTypeName(), ex);
    }
  }

  @Override
  public List<Parameter> getModelParameters(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return loadUnconfiguredMissionModel(missionModelId).getParameters();
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named mission model.
   *
   * @param message The parameters defining the simulation to perform.
   * @return A set of samples over the course of the simulation.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  @Override
  public SimulationResults runSimulation(final CreateSimulationMessage message)
  throws NoSuchMissionModelException
  {
    final var config = message.configuration();
    if (config.isEmpty()) {
      log.warning(
          "No mission model configuration defined for mission model. Simulations will receive an empty set of configuration arguments.");
    }

    // TODO: [AERIE-1516] Teardown the mission model after use to release any system resources (e.g. threads).
    return loadConfiguredMissionModel(message.missionModelId(), SerializedValue.of(config))
        .simulate(message.activityInstances(), message.samplingDuration(), message.startTime());
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
      this.missionModelRepository.updateActivityTypes(missionModelId, getActivityTypes(missionModelId));
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    }
  }

  private MissionModelFacade.Unconfigured<?> loadUnconfiguredMissionModel(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    try {
      final var missionModelJar = this.missionModelRepository.getMissionModel(missionModelId);
      final var missionModel =
          MissionModelLoader.loadMissionModelFactory(missionModelDataPath.resolve(missionModelJar.path), missionModelJar.name, missionModelJar.version);
      return new MissionModelFacade.Unconfigured<>(missionModel);
    } catch (final MissionModelRepository.NoSuchMissionModelException ex) {
      throw new NoSuchMissionModelException(missionModelId, ex);
    } catch (final MissionModelLoader.MissionModelLoadException ex) {
      throw new MissionModelLoadException(ex);
    }
  }

  /**
   * Load an {@link MissionModel} from the mission model repository using the mission model's default mission model configuration,
   * and wrap it in an {@link MissionModelFacade} domain object.
   *
   * @param missionModelId The ID of the mission model in the mission model repository to load.
   * @return An {@link MissionModelFacade} domain object allowing use of the loaded mission model.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  private MissionModelFacade loadConfiguredMissionModel(final String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    return loadConfiguredMissionModel(missionModelId, SerializedValue.of(Map.of()));
  }

  /**
   * Load an {@link MissionModel} from the mission model repository, and wrap it in an {@link MissionModelFacade} domain object.
   *
   * @param missionModelId The ID of the mission model in the mission model repository to load.
   * @param configuration The mission model configuration to load the mission model with.
   * @return An {@link MissionModelFacade} domain object allowing use of the loaded mission model.
   * @throws MissionModelLoadException If the mission model cannot be loaded -- the JAR may be invalid, or the mission model
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchMissionModelException If no mission model is known by the given ID.
   */
  private MissionModelFacade loadConfiguredMissionModel(final String missionModelId, final SerializedValue configuration)
  throws NoSuchMissionModelException, MissionModelLoadException
  {
    try {
      final var missionModelJar = this.missionModelRepository.getMissionModel(missionModelId);
      final var missionModel =
          MissionModelLoader.loadMissionModel(configuration, missionModelDataPath.resolve(missionModelJar.path), missionModelJar.name, missionModelJar.version);
      return new MissionModelFacade(missionModel);
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
