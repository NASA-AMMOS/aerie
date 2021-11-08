package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationLoader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implements the adaptation service {@link AdaptationService} interface on a set of local domain objects.
 *
 * May throw unchecked exceptions:
 * * {@link LocalAdaptationService.AdaptationLoadException}: When an adaptation cannot be loaded from the JAR provided by the
 * connected
 * adaptation repository.
 */
public final class LocalAdaptationService implements AdaptationService {
  private static final Logger log = Logger.getLogger(LocalAdaptationService.class.getName());

  private final Path missionModelDataPath;
  private final AdaptationRepository adaptationRepository;

  public LocalAdaptationService(
      final Path missionModelDataPath,
      final AdaptationRepository adaptationRepository
  ) {
    this.missionModelDataPath = missionModelDataPath;
    this.adaptationRepository = adaptationRepository;
  }

  @Override
  public Map<String, AdaptationJar> getAdaptations() {
    return this.adaptationRepository.getAllAdaptations();
  }

  @Override
  public AdaptationJar getAdaptationById(String id) throws NoSuchAdaptationException {
    try {
      return this.adaptationRepository.getAdaptation(id);
    } catch (AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(id, ex);
    }
  }

  @Override
  public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
    try {
      return this.adaptationRepository.getConstraints(adaptationId);
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(adaptationId, ex);
    }
  }

  @Override
  public Map<String, ValueSchema> getStatesSchemas(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    // TODO: [AERIE-1516] Teardown the adaptation after use to release any system resources (e.g. threads).
    return loadConfiguredAdaptation(adaptationId).getStateSchemas();
  }

  /**
   * Get information about all activity types in the named adaptation.
   *
   * @param adaptationId The ID of the adaptation to load.
   * @return The set of all activity types in the named adaptation, indexed by name.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public Map<String, ActivityType> getActivityTypes(String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    return loadUnconfiguredAdaptation(adaptationId)
        .getActivityTypes();
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named adaptation.
   *
   * @param adaptationId The ID of the adaptation to load.
   * @param activityParameters The serialized activity to validate against the named adaptation.
   * @return A list of validation errors that is empty if validation succeeds.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   * @throws AdaptationFacade.AdaptationContractException If the named adaptation does not abide by the expected contract.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public List<String> validateActivityParameters(final String adaptationId, final SerializedActivity activityParameters)
  throws NoSuchAdaptationException, AdaptationFacade.AdaptationContractException, AdaptationLoadException
  {
    try {
      // TODO: [AERIE-1516] Teardown the adaptation after use to release any system resources (e.g. threads).
      return this.loadConfiguredAdaptation(adaptationId)
                 .validateActivity(activityParameters.getTypeName(), activityParameters.getParameters());
    } catch (final AdaptationFacade.NoSuchActivityTypeException ex) {
      return List.of("unknown activity type");
    } catch (final AdaptationFacade.UnconstructableActivityInstanceException ex) {
      return List.of(ex.getMessage());
    }
  }

  @Override
  public List<Parameter> getModelParameters(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    return loadUnconfiguredAdaptation(adaptationId).getParameters();
  }

  /**
   * Validate that a set of activity parameters conforms to the expectations of a named adaptation.
   *
   * @param message The parameters defining the simulation to perform.
   * @return A set of samples over the course of the simulation.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   */
  @Override
  public SimulationResults runSimulation(final CreateSimulationMessage message)
  throws NoSuchAdaptationException
  {
    final var config = message.configuration();
    if (config.isEmpty()) {
      log.warning(
          "No mission model configuration defined for adaptation. Simulations will receive an empty set of configuration arguments.");
    }

    // TODO: [AERIE-1516] Teardown the adaptation after use to release any system resources (e.g. threads).
    return loadConfiguredAdaptation(message.adaptationId(), SerializedValue.of(config))
        .simulate(message.activityInstances(), message.samplingDuration(), message.startTime());
  }

  @Override
  public void refreshModelParameters(final String adaptationId)
  throws NoSuchAdaptationException
  {
    try {
      this.adaptationRepository.updateModelParameters(adaptationId, getModelParameters(adaptationId));
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(adaptationId, ex);
    }
  }

  @Override
  public void refreshActivityTypes(final String adaptationId)
  throws NoSuchAdaptationException
  {
    try {
      this.adaptationRepository.updateActivityTypes(adaptationId, getActivityTypes(adaptationId));
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(adaptationId, ex);
    }
  }

  private AdaptationFacade.Unconfigured<?> loadUnconfiguredAdaptation(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    try {
      final var adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
      final var adaptation =
          AdaptationLoader.loadAdaptationFactory(missionModelDataPath.resolve(adaptationJar.path), adaptationJar.name, adaptationJar.version);
      return new AdaptationFacade.Unconfigured<>(adaptation);
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(adaptationId, ex);
    } catch (final AdaptationLoader.AdaptationLoadException ex) {
      throw new AdaptationLoadException(ex);
    }
  }

  /**
   * Load an {@link Adaptation} from the adaptation repository using the adaptation's default mission model configuration,
   * and wrap it in an {@link AdaptationFacade} domain object.
   *
   * @param adaptationId The ID of the adaptation in the adaptation repository to load.
   * @return An {@link AdaptationFacade} domain object allowing use of the loaded adaptation.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   */
  private AdaptationFacade<?> loadConfiguredAdaptation(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    return loadConfiguredAdaptation(adaptationId, SerializedValue.of(Map.of()));
  }

  /**
   * Load an {@link Adaptation} from the adaptation repository, and wrap it in an {@link AdaptationFacade} domain object.
   *
   * @param adaptationId The ID of the adaptation in the adaptation repository to load.
   * @param configuration The mission model configuration to to load the adaptation with.
   * @return An {@link AdaptationFacade} domain object allowing use of the loaded adaptation.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   */
  private AdaptationFacade<?> loadConfiguredAdaptation(final String adaptationId, final SerializedValue configuration)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    try {
      final var adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
      final var adaptation =
          AdaptationLoader.loadAdaptation(configuration, missionModelDataPath.resolve(adaptationJar.path), adaptationJar.name, adaptationJar.version);
      return new AdaptationFacade<>(adaptation);
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(adaptationId, ex);
    } catch (final AdaptationLoader.AdaptationLoadException ex) {
      throw new AdaptationLoadException(ex);
    }
  }

  public static class AdaptationLoadException extends RuntimeException {
    public AdaptationLoadException(final Throwable cause) { super(cause); }
  }
}
