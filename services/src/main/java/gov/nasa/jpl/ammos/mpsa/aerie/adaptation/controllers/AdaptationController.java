package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class AdaptationController implements IAdaptationController {
    private final AdaptationRepository adaptationRepository;

    public AdaptationController(final AdaptationRepository adaptationRepository) {
        this.adaptationRepository = adaptationRepository;
    }

    @Override
    public Stream<Pair<String, Adaptation>> getAdaptations() {
        return this.adaptationRepository.getAllAdaptations();
    }

    @Override
    public Adaptation getAdaptationById(String id) throws NoSuchAdaptationException {
        return this.adaptationRepository.getAdaptation(id);
    }

    @Override
    public String addAdaptation(NewAdaptation adaptation) throws ValidationException {
        validateAdaptation(adaptation);
        return this.adaptationRepository.createAdaptation(adaptation);
    }

    @Override
    public void removeAdaptation(String id) throws NoSuchAdaptationException {
        this.adaptationRepository.deleteAdaptation(id);
    }

    @Override
    public Map<String, ActivityType> getActivityTypes(String adaptationId) throws NoSuchAdaptationException, AdaptationContractException {
        final Adaptation adaptation = this.adaptationRepository.getAdaptation(adaptationId);

        return AdaptationLoader.loadActivities(adaptation.path);
    }

    @Override
    public ActivityType getActivityType(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationContractException {
        final Map<String, ActivityType> activityTypes = getActivityTypes(adaptationId);

        return Optional
            .ofNullable(activityTypes.getOrDefault(activityTypeId, null))
            .orElseThrow(() -> new NoSuchActivityTypeException(adaptationId, activityTypeId));
    }

    @Override
    public Map<String, ParameterSchema> getActivityTypeParameters(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationContractException {
        return getActivityType(adaptationId, activityTypeId).parameters;
    }

    @Override
    public Activity<?> instantiateActivity(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, AdaptationContractException, NoSuchActivityTypeException,
        UnconstructableActivityInstanceException
    {
        final Adaptation adaptationInfo = this.adaptationRepository.getAdaptation(adaptationId);
        final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationInfo.path);

        return deserializeActivity(adaptationId, adaptation.getActivityMapper(), activityParameters);
    }

    private Activity<?> deserializeActivity(final String adaptationId, final ActivityMapper activityMapper, final SerializedActivity activityParameters)
        throws AdaptationContractException, NoSuchActivityTypeException, UnconstructableActivityInstanceException
    {
        final Activity<?> activity;
        {
            Optional<Activity<?>> mapperResult;
            try {
                mapperResult = activityMapper.deserializeActivity(activityParameters);
            } catch (final RuntimeException ex) {
                // It's a serious code smell that failures of `deserializeActivity`
                // have no outlet other than as unchecked exceptions.
                throw new UnconstructableActivityInstanceException(
                    "Unknown failure when deserializing activity -- do the parameters match the schema?",
                    ex);
            }

            if (mapperResult == null) {
                throw new AdaptationContractException(activityMapper.getClass().getName() + ".deserializeActivity() returned null");
            } else if (mapperResult.isEmpty()) {
                throw new NoSuchActivityTypeException(adaptationId, activityParameters.getTypeName());
            }

            activity = mapperResult.get();
        }

        return activity;
    }

    private void validateAdaptation(final NewAdaptation adaptation) throws ValidationException {
        final List<String> validationErrors = new ArrayList<>();

        if (adaptation.name == null) validationErrors.add("name must be non-null");
        if (adaptation.version == null) validationErrors.add("version must be non-null");

        if (adaptation.path == null) {
            validationErrors.add("path must be non-null");
        } else {
            Map<String, ActivityType> activities = null;
            try {
                activities = AdaptationLoader.loadActivities(adaptation.path);
            } catch (final AdaptationContractException e) {
                validationErrors.add("Adaptation JAR does not meet contract: " + e.getMessage());
            }

            if (activities != null && activities.size() < 1) validationErrors.add("No activities found. Must include at least one activity");
        }

        if (validationErrors.size() > 0) {
            throw new ValidationException("invalid adaptation", validationErrors);
        }
    }
}
