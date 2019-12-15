package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.app;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.UnconstructableActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
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

public class LocalApp implements App {
    private final AdaptationRepository adaptationRepository;

    public LocalApp(final AdaptationRepository adaptationRepository) {
        this.adaptationRepository = adaptationRepository;
    }

    @Override
    public Stream<Pair<String, AdaptationJar>> getAdaptations() {
        return this.adaptationRepository.getAllAdaptations();
    }

    @Override
    public AdaptationJar getAdaptationById(String id) throws NoSuchAdaptationException {
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
        final AdaptationJar adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);

        final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationJar.path);
        final ActivityMapper activityMapper = adaptation.getActivityMapper();
        if (activityMapper == null) throw new AdaptationContractException(adaptation.getClass().getCanonicalName() + ".getActivityMapper() returned null");

        final Map<String, Map<String, ParameterSchema>> activitySchemas = activityMapper.getActivitySchemas();
        if (activitySchemas == null) throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".getActivitySchemas() returned null");

        final Map<String, ActivityType> activityTypes = new HashMap<>();
        for (final var schema : activitySchemas.entrySet()) {
            final Activity<?> activity;
            try {
                activity = deserializeActivity(adaptationId, activityMapper, new SerializedActivity(schema.getKey(), Collections.emptyMap()));
            } catch (final NoSuchActivityTypeException ex) {
                throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".deserializeActivity() returned an empty Optional for an activity type it has a schema for", ex);
            } catch (final UnconstructableActivityInstanceException ex) {
                throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".deserializeActivity() could not instantiate an activity with only default parameters", ex);
            }

            final Optional<SerializedActivity> defaultActivity = activityMapper.serializeActivity(activity);
            if (defaultActivity.isEmpty()) throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".serializeActivity() returned an empty Optional for an activity type it previously deserialized");

            activityTypes.put(schema.getKey(), new ActivityType(schema.getKey(), schema.getValue(), defaultActivity.get().getParameters()));
        }

        return activityTypes;
    }

    @Override
    public ActivityType getActivityType(String adaptationId, String activityTypeId) throws NoSuchAdaptationException, NoSuchActivityTypeException, AdaptationContractException {
        final AdaptationJar adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
        final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationJar.path);

        final ActivityMapper activityMapper = adaptation.getActivityMapper();
        if (activityMapper == null) throw new AdaptationContractException(adaptation.getClass().getCanonicalName() + ".getActivityMapper() returned null");

        final Map<String, Map<String, ParameterSchema>> activitySchemas = activityMapper.getActivitySchemas();
        if (activitySchemas == null) throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".getActivitySchemas() returned null");

        final Map<String, ParameterSchema> activitySchema = activitySchemas.getOrDefault(activityTypeId, null);
        if (activitySchema == null) throw new NoSuchActivityTypeException(adaptationId, activityTypeId);

        final Activity<?> activity;
        try {
            activity = deserializeActivity(adaptationId, activityMapper, new SerializedActivity(activityTypeId, Collections.emptyMap()));
        } catch (final NoSuchActivityTypeException ex) {
            throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".deserializeActivity() returned an empty Optional for an activity type it has a schema for", ex);
        } catch (final UnconstructableActivityInstanceException ex) {
            throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".deserializeActivity() could not instantiate an activity with only default parameters", ex);
        }

        final Optional<SerializedActivity> defaultActivity = activityMapper.serializeActivity(activity);
        if (defaultActivity.isEmpty()) throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".serializeActivity() returned an empty Optional for an activity type it previously deserialized");

        return new ActivityType(activityTypeId, activitySchemas.get(activityTypeId), defaultActivity.get().getParameters());
    }


    @Override
    public Activity<?> instantiateActivity(final String adaptationId, final SerializedActivity activityParameters)
        throws NoSuchAdaptationException, AdaptationContractException, NoSuchActivityTypeException,
        UnconstructableActivityInstanceException
    {
        final AdaptationJar adaptationJar = this.adaptationRepository.getAdaptation(adaptationId);
        final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationJar.path);

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

    private void validateAdaptation(final NewAdaptation adaptationDescriptor) throws ValidationException {
        final List<String> validationErrors = new ArrayList<>();

        if (adaptationDescriptor.name == null) validationErrors.add("name must be non-null");
        if (adaptationDescriptor.version == null) validationErrors.add("version must be non-null");

        if (adaptationDescriptor.path == null) {
            validationErrors.add("path must be non-null");
        } else {
            try {
                final MerlinAdaptation<?> adaptation = AdaptationLoader.loadAdaptation(adaptationDescriptor.path);

                final ActivityMapper activityMapper = adaptation.getActivityMapper();
                if (activityMapper == null) throw new AdaptationContractException(adaptation.getClass().getCanonicalName() + ".getActivityMapper() returned null");

                final Map<String, Map<String, ParameterSchema>> activitySchemas = activityMapper.getActivitySchemas();
                if (activitySchemas == null) throw new AdaptationContractException(activityMapper.getClass().getCanonicalName() + ".getActivitySchemas() returned null");

                if (activitySchemas.size() < 1) validationErrors.add("No activities found. Must include at least one activity");
            } catch (final AdaptationContractException ex) {
                validationErrors.add("Adaptation JAR does not meet contract: " + ex.getMessage());
            }
        }

        if (validationErrors.size() > 0) {
            throw new ValidationException("invalid adaptation", validationErrors);
        }
    }
}
