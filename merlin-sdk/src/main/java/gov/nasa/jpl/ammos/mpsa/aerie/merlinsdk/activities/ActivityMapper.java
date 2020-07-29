package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;

import java.util.Map;
import java.util.Optional;

/**
 * A mission-agnostic representation of the activity types and activity instances
 * defined by a mission adaptation.
 */
public interface ActivityMapper {
  /**
   * Gets a set of named schemas for each activity type understood by this ActivityMapper.
   *
   * @return A set of named activity schemas.
   */
  Map<String, Map<String, ValueSchema>> getActivitySchemas();

  /**
   * Produces an adaptation-specific activity domain object from a mission-agnostic representation.
   *
   * @param serializedActivity A mission-agnostic representation of an activity instance.
   * @return An adaptation-specific activity instance implementing the {@link Activity} interface,
   *   or an empty {@link Optional} if this mapper does not know the activity type named
   *   by {@code activity.getTypeName()}.
   */
  Optional<Activity> deserializeActivity(SerializedActivity serializedActivity);

  /**
   * Produces a mission-agnostic representation of an adaptation-specific activity domain object.
   *
   * @param activity An adaptation-specific domain object implementing the {@link Activity} interface.
   * @return A mission-agnostic representation of {@code activity}, or an empty {@link Optional}
   *   if this mapper does not understand the provided activity instance.
   */
  Optional<SerializedActivity> serializeActivity(Activity activity);
}
