package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.ViolableConstraint;
import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.NewAdaptation;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.utilities.AdaptationLoader;
import io.javalin.core.util.FileUtil;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Implements the plan service {@link AdaptationService} interface on a set of local domain objects.
 *
 * May throw unchecked exceptions:
 * * {@link LocalAdaptationService.AdaptationLoadException}: When an adaptation cannot be loaded from the JAR provided by the
 * connected
 * adaptation repository.
 */
public final class LocalAdaptationService implements AdaptationService {
  private final AdaptationRepository adaptationRepository;

  public LocalAdaptationService(final AdaptationRepository adaptationRepository) {
    this.adaptationRepository = adaptationRepository;
  }

  @Override
  public Map<String, AdaptationJar> getAdaptations() {
    return this.adaptationRepository.getAllAdaptations().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
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
  public String addAdaptation(NewAdaptation adaptation) throws AdaptationRejectedException {
    final Path path;
    try {
      path = Files.createTempFile("adaptation", ".jar");
    } catch (final IOException ex) {
      throw new Error(ex);
    }
    FileUtil.streamToFile(adaptation.jarSource, path.toString());

    final String adClassStr;
    try {
      adClassStr = getImplementingClassName(path, AdaptationFactory.class);
    } catch (final IOException ex) {
      throw new AdaptationRejectedException(ex);
    }

    try {
      final var adClass = new ClassParser(path.toString(), getClasspathRelativePath(adClassStr));
      final var javaAdClass = adClass.parse();

      if (!isClassCompatibleWithThisVM(javaAdClass)) {
        throw new AdaptationRejectedException(String.format(
            "Adaptation was compiled with an older Java version. Please compile with Java %d.",
            Runtime.version().feature()));
      }
    } catch (final IOException ex) {
      throw new AdaptationRejectedException(ex);
    }

    try {
      AdaptationLoader.loadAdaptationProvider(path, adaptation.name, adaptation.version);
    } catch (final AdaptationLoader.AdaptationLoadException ex) {
      throw new AdaptationRejectedException(ex);
    }

    final AdaptationJar adaptationJar = new AdaptationJar();
    adaptationJar.name = adaptation.name;
    adaptationJar.version = adaptation.version;
    adaptationJar.mission = adaptation.mission;
    adaptationJar.owner = adaptation.owner;
    adaptationJar.path = path;

    return this.adaptationRepository.createAdaptation(adaptationJar);
  }

  @Override
  public void removeAdaptation(String id) throws NoSuchAdaptationException {
    try {
      this.adaptationRepository.deleteAdaptation(id);
    } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
      throw new NoSuchAdaptationException(id, ex);
    }
  }

  @Override
  public List<ViolableConstraint> getConstraintTypes(final String adaptationID)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    return loadAdaptation(adaptationID).getConstraintTypes();
  }

  @Override
  public Map<String, ValueSchema> getStatesSchemas(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    return loadAdaptation(adaptationId).getStateSchemas();
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
    return loadAdaptation(adaptationId)
        .getActivityTypes();
  }

  /**
   * Get information about the named activity type in the named adaptation.
   *
   * @param adaptationId The ID of the adaptation to load.
   * @param activityTypeId The ID of the activity type to query in the named adaptation.
   * @return Information about the named activity type.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   * @throws NoSuchActivityTypeException If no activity type exists for the given serialized activity.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   */
  @Override
  public ActivityType getActivityType(String adaptationId, String activityTypeId)
  throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationLoadException
  {
    try {
      return loadAdaptation(adaptationId).getActivityType(activityTypeId);
    } catch (final AdaptationFacade.NoSuchActivityTypeException ex) {
      throw new NoSuchActivityTypeException(activityTypeId, ex);
    }
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
      return this.loadAdaptation(adaptationId)
                 .validateActivity(activityParameters.getTypeName(), activityParameters.getParameters());
    } catch (final AdaptationFacade.NoSuchActivityTypeException ex) {
      return List.of("unknown activity type");
    } catch (final AdaptationFacade.UnconstructableActivityInstanceException ex) {
      return List.of(ex.getMessage());
    }
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
  throws NoSuchAdaptationException,
         SimulationDriver.TaskSpecInstantiationException
  {
    return loadAdaptation(message.adaptationId)
        .simulate(message.activityInstances, message.samplingDuration, message.startTime);
  }

  private static String getImplementingClassName(final Path jarPath, final Class<?> javaClass)
  throws IOException, AdaptationRejectedException {
    final var jarFile = new JarFile(jarPath.toFile());
    final var jarEntry = jarFile.getEntry("META-INF/services/" + javaClass.getCanonicalName());
    final var inputStream = jarFile.getInputStream(jarEntry);

    final var classPathList = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        .lines()
        .collect(Collectors.toList());

    if (classPathList.size() != 1) {
      throw new AdaptationRejectedException(
          "Adaptation contains zero/multiple registered implementations of AdaptationFactory.");
    }

    return classPathList.get(0);
  }

  private static String getClasspathRelativePath(final String className) {
    return className.replaceAll("\\.", "/").concat(".class");
  }

  private static boolean isClassCompatibleWithThisVM(final JavaClass javaClass) {
    // Refer to this link https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html
    return Runtime.version().feature() + 44 >= javaClass.getMajor();
  }

  /**
   * Load an {@link Adaptation} from the adaptation repository, and wrap it in an {@link AdaptationFacade} domain object.
   *
   * @param adaptationId The ID of the adaptation in the adaptation repository to load.
   * @return An {@link AdaptationFacade} domain object allowing use of the loaded adaptation.
   * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
   * it contains may not abide by the expected contract at load time.
   * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
   */
  private AdaptationFacade<?> loadAdaptation(final String adaptationId)
  throws NoSuchAdaptationException, AdaptationLoadException
  {
    try {
      final var adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
      final var adaptation =
          AdaptationLoader.loadAdaptation(adaptationJar.path, adaptationJar.name, adaptationJar.version);
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
