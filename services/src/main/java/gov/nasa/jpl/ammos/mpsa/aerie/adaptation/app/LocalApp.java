package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import io.javalin.core.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the plan service {@link App} interface on a set of local domain objects.
 *
 * May throw unchecked exceptions:
 * * {@link LocalApp.AdaptationLoadException}: When an adaptation cannot be loaded from the JAR provided by the connected
 *   adaptation repository.
 */
public final class LocalApp implements App {
    private final AdaptationRepository adaptationRepository;

    public LocalApp(final AdaptationRepository adaptationRepository) {
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

    /**
     * Get information about all activity types in the named adaptation.
     *
     * @param adaptationId The ID of the adaptation to load.
     * @return The set of all activity types in the named adaptation, indexed by name.
     * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
     * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
     *         it contains may not abide by the expected contract at load time.
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
     *         it contains may not abide by the expected contract at load time.
     */
    @Override
    public ActivityType getActivityType(String adaptationId, String activityTypeId)
        throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationLoadException
    {
        try {
            return loadAdaptation(adaptationId).getActivityType(activityTypeId);
        } catch (final Adaptation.NoSuchActivityTypeException ex) {
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
     * @throws Adaptation.AdaptationContractException If the named adaptation does not abide by the expected contract.
     * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
     *         it contains may not abide by the expected contract at load time.
     */
    @Override
    public List<String> validateActivityParameters(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, Adaptation.AdaptationContractException, AdaptationLoadException
    {
        final Activity activity;
        try {
            activity = this.loadAdaptation(adaptationId).instantiateActivity(activityParameters);
        } catch (final Adaptation.NoSuchActivityTypeException ex) {
            return List.of("unknown activity type");
        } catch (final Adaptation.UnconstructableActivityInstanceException ex) {
            return List.of(ex.getMessage());
        }

        final List<String> failures = activity.validateParameters();
        if (failures == null) {
            // TODO: The top-level application layer is a poor place to put knowledge about the adaptation contract.
            //   Move this logic somewhere better.
            throw new Adaptation.AdaptationContractException(activity.getClass().getName() + ".validateParameters() returned null");
        }

        return failures;
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
        // TODO: hide these exceptions behind a façade.
        Adaptation.UnconstructableActivityInstanceException, Adaptation.NoSuchActivityTypeException
    {
        final var adaptation = loadAdaptation(message.adaptationId);
        final var simulator = new Simulator(adaptation);

        final var activityInstances = new ArrayList<Pair<Duration, Activity>>(message.activityInstances.size());
        for (final var entry : message.activityInstances) {
            final var startDelta = entry.getLeft();
            final var serializedInstance = entry.getRight();

            activityInstances.add(Pair.of(startDelta, adaptation.instantiateActivity(serializedInstance)));
        }

        return simulator.run(message.samplingDuration, message.samplingPeriod, activityInstances);
    }

    /**
     * Load a {@link MerlinAdaptation} from the adaptation repository, and wrap it in an {@link Adaptation} domain object.
     *
     * @param adaptationId The ID of the adaptation in the adaptation repository to load.
     * @return An {@link Adaptation} domain object allowing use of the loaded adaptation.
     * @throws AdaptationLoadException If the adaptation cannot be loaded -- the JAR may be invalid, or the adaptation
     *         it contains may not abide by the expected contract at load time.
     * @throws NoSuchAdaptationException If no adaptation is known by the given ID.
     */
    private Adaptation loadAdaptation(final String adaptationId) throws NoSuchAdaptationException, AdaptationLoadException {
        try {
            final AdaptationJar adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
            final MerlinAdaptation adaptation =
                AdaptationLoader.loadAdaptation(adaptationJar.path, adaptationJar.name, adaptationJar.version);
            return new Adaptation(adaptation);
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
