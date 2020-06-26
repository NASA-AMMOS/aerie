package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReplayingSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Adaptation<Event> {
    private final MerlinAdaptation<Event> adaptation;
    private final ActivityMapper activityMapper;

    public Adaptation(final MerlinAdaptation<Event> adaptation) throws AdaptationContractException {
      this.adaptation = adaptation;
      this.activityMapper = this.adaptation.getActivityMapper();

      if (this.activityMapper == null) {
        throw new AdaptationContractException(this.adaptation.getClass().getCanonicalName() + ".getActivityMapper() returned null");
      }
    }

    public SimulationResults simulate(
        final Collection<Pair<Duration, SerializedActivity>> schedule,
        final Duration simulationDuration,
        final Duration samplingPeriod
    ) {
        return SimpleSimulator.simulate(this.adaptation, schedule, simulationDuration, samplingPeriod);
    }

    public Map<String, ActivityType> getActivityTypes() throws AdaptationContractException {
        final Map<String, Map<String, ParameterSchema>> activitySchemas = this.activityMapper.getActivitySchemas();
        if (activitySchemas == null) throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".getActivitySchemas() returned null");

        final Map<String, ActivityType> activityTypes = new HashMap<>();
        for (final var schema : activitySchemas.entrySet()) {
            final Activity activity;
            try {
                activity = instantiateActivity(new SerializedActivity(schema.getKey(), Collections.emptyMap()));
            } catch (final NoSuchActivityTypeException ex) {
                throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".deserializeActivity() returned an empty Optional for an activity type it has a schema for", ex);
            } catch (final UnconstructableActivityInstanceException ex) {
                throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".deserializeActivity() could not instantiate an activity with only default parameters", ex);
            }

            final Optional<SerializedActivity> defaultActivity = this.activityMapper.serializeActivity(activity);
            if (defaultActivity.isEmpty()) throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".serializeActivity() returned an empty Optional for an activity type it previously deserialized");

            activityTypes.put(schema.getKey(), new ActivityType(schema.getKey(), schema.getValue(), defaultActivity.get().getParameters()));
        }

        return activityTypes;
    }

    public ActivityType getActivityType(final String activityTypeId) throws NoSuchActivityTypeException, AdaptationContractException {
        final Map<String, Map<String, ParameterSchema>> activitySchemas = this.activityMapper.getActivitySchemas();
        if (activitySchemas == null) throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".getActivitySchemas() returned null");

        final Map<String, ParameterSchema> activitySchema = activitySchemas.getOrDefault(activityTypeId, null);
        if (activitySchema == null) throw new NoSuchActivityTypeException();

        final Activity activity;
        try {
            activity = instantiateActivity(new SerializedActivity(activityTypeId, Collections.emptyMap()));
        } catch (final NoSuchActivityTypeException ex) {
            throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".deserializeActivity() returned an empty Optional for an activity type it has a schema for", ex);
        } catch (final UnconstructableActivityInstanceException ex) {
            throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".deserializeActivity() could not instantiate an activity with only default parameters", ex);
        }

        final Optional<SerializedActivity> defaultActivity = this.activityMapper.serializeActivity(activity);
        if (defaultActivity.isEmpty()) throw new AdaptationContractException(this.activityMapper.getClass().getCanonicalName() + ".serializeActivity() returned an empty Optional for an activity type it previously deserialized");

        return new ActivityType(activityTypeId, activitySchemas.get(activityTypeId), defaultActivity.get().getParameters());
    }

    public Activity instantiateActivity(final SerializedActivity activityParameters)
        throws AdaptationContractException, NoSuchActivityTypeException, UnconstructableActivityInstanceException
    {
        Optional<Activity> mapperResult;
        try {
            mapperResult = this.activityMapper.deserializeActivity(activityParameters);
        } catch (final RuntimeException ex) {
            // It's a serious code smell that failures of `deserializeActivity`
            // have no outlet other than as unchecked exceptions.
            throw new UnconstructableActivityInstanceException(
                "Unknown failure when deserializing activity -- do the parameters match the schema?",
                ex);
        }

        if (mapperResult == null) {
            throw new AdaptationContractException(this.activityMapper.getClass().getName() + ".deserializeActivity() returned null");
        }

        return mapperResult
            .orElseThrow(NoSuchActivityTypeException::new);
    }

    public static class AdaptationContractException extends RuntimeException {
        public AdaptationContractException(final String message) {
            super(message);
        }

        public AdaptationContractException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class NoSuchActivityTypeException extends Exception {}

    public static class UnconstructableActivityInstanceException extends Exception {
        public UnconstructableActivityInstanceException(final String message) {
            super(message);
        }

        public UnconstructableActivityInstanceException(final String message, final Throwable cause) { super(message, cause); }
    }
}
