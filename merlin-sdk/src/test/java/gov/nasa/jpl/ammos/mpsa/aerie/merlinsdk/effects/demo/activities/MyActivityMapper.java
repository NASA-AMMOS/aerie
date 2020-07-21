package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

import java.util.Map;
import java.util.Optional;

public final class MyActivityMapper implements ActivityMapper {
  @Override
  public Map<String, Map<String, ParameterSchema>> getActivitySchemas() {
    return Map.of("a", Map.of(), "b", Map.of(), "c", Map.of());
  }

  @Override
  public Optional<Activity> deserializeActivity(final SerializedActivity serializedActivity) {
    switch (serializedActivity.getTypeName()) {
      case "a": return Optional.of(new ActivityA());
      case "b": return Optional.of(new ActivityB());
      default: return Optional.of(new Activity() {});
    }
  }

  @Override
  public Optional<SerializedActivity> serializeActivity(final Activity activity) {
    if (activity instanceof ActivityA) return Optional.of(new SerializedActivity("a", Map.of()));
    else if (activity instanceof ActivityB) return Optional.of(new SerializedActivity("b", Map.of()));
    else return Optional.of(new SerializedActivity("c", Map.of()));
  }
}
