package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;

public class CompositeActivityMapper implements ActivityMapper {
  private final Map<String, ActivityMapper> activityMappers;
  private final Map<String, Map<String, ParameterSchema>> activitySchemas;

  public CompositeActivityMapper(final Map<String, ActivityMapper> activityMappers) {
    this.activityMappers = activityMappers;
    this.activitySchemas = immutableActivitySchemas(activityMappers);
  }

  private Optional<ActivityMapper> lookupMapper(final String activityType) {
    return Optional.ofNullable(this.activityMappers.get(activityType));
  }


  @Override
  public Map<String, Map<String, ParameterSchema>> getActivitySchemas() {
    return this.activitySchemas;
  }

  @Override
  public Optional<Activity<? extends StateContainer>> deserializeActivity(final SerializedActivity activity) {
    final String activityType = activity.getTypeName();
    return lookupMapper(activityType).flatMap(m -> m.deserializeActivity(activity));
  }

  @Override
  public Optional<SerializedActivity> serializeActivity(final Activity activity) {
    final String activityType = activity.getClass().getAnnotation(ActivityType.class).name();
    return lookupMapper(activityType).flatMap(m -> m.serializeActivity(activity));
  }

  private static Map<String, Map<String, ParameterSchema>> immutableActivitySchemas(final Map<String, ActivityMapper> activityMappers) {
    final Map<String, Map<String, ParameterSchema>> clonedSchemas = new HashMap<>();

    for (final var mapper : activityMappers.values()) {
      for (final var activityEntry : mapper.getActivitySchemas().entrySet()) {
        final String activityName = activityEntry.getKey();
        final Map<String, ParameterSchema> activityParameters = new HashMap<>(activityEntry.getValue());

        clonedSchemas.put(activityName, activityParameters);
      }
    }

    return unmodifiableMap(clonedSchemas);
  }
}
