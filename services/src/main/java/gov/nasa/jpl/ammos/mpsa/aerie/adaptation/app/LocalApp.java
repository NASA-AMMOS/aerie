package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import io.javalin.core.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            AdaptationLoader.loadAdaptationProvider(path);
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
    public Map<String, ActivityType> getActivityTypes(String adaptationId)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException
    {
        return loadAdaptation(adaptationId)
            .getActivityTypes();
    }

    @Override
    public ActivityType getActivityType(String adaptationId, String activityTypeId)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException,
        NoSuchActivityTypeException
    {
        return loadAdaptation(adaptationId)
            .getActivityType(activityTypeId);
    }

    @Override
    public List<String> validateActivityParameters(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException,
        NoSuchActivityTypeException, UnconstructableActivityInstanceException
    {
        final Activity<?> activity = loadAdaptation(adaptationId)
            .instantiateActivity(activityParameters);

        final List<String> failures = activity.validateParameters();
        if (failures == null) {
            // TODO: The top-level application layer is a poor place to put knowledge about the adaptation contract.
            //   Move this logic somewhere better.
            throw new Adaptation.AdaptationContractException(activity.getClass().getName() + ".validateParameters() returned null");
        }

        return failures;
    }

    private Adaptation loadAdaptation(final String adaptationId) throws NoSuchAdaptationException, AdaptationLoader.AdaptationLoadException, Adaptation.AdaptationContractException {
        try {
            final AdaptationJar adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
            final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationJar.path);
            return new Adaptation(adaptation);
        } catch (final AdaptationRepository.NoSuchAdaptationException ex) {
            throw new NoSuchAdaptationException(adaptationId, ex);
        }
    }
}
