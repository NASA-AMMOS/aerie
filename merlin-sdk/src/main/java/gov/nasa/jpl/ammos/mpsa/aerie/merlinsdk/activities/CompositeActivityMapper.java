package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;

public class CompositeActivityMapper implements ActivityMapper {
  private final Map<String, ActivityMapper> activityMappers;
  private final Map<String, ParameterSchema> activitySchemas = new HashMap<>();

  public CompositeActivityMapper(final Map<String, ActivityMapper> activityMappers) {
    this.activityMappers = activityMappers;
    for (final var mapper : this.activityMappers.values()) {
      this.activitySchemas.putAll(mapper.getActivitySchemas());
    }
  }

  private Optional<ActivityMapper> lookupMapper(final String activityType) {
    return Optional.ofNullable(this.activityMappers.get(activityType));
  }


  @Override
  public Map<String, ParameterSchema> getActivitySchemas() {
    return unmodifiableMap(this.activitySchemas);
  }

  @Override
  public Optional<Activity<? extends StateContainer>> deserializeActivity(final SerializedActivity activity) {
    final String activityType = activity.getTypeName();
    return lookupMapper(activityType).flatMap(m -> m.deserializeActivity(activity));
  }

  @Override
  public Optional<SerializedActivity> serializeActivity(final Activity activity) {
    final String activityType = activity.getClass().getAnnotation(ActivityType.class).value();
    return lookupMapper(activityType).flatMap(m -> m.serializeActivity(activity));
  }
}
