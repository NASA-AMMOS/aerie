package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.mappers;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivitiesMapped;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ActivitiesMapped({PeelBananaActivity.class})
public final class PeelBananaActivityMapper implements ActivityMapper {
  private static final String ACTIVITY_TYPE = "PeelBanana";

  @Override
  public Map<String, Map<String, ParameterSchema>> getActivitySchemas() {
    final var parameters = new HashMap<String, ParameterSchema>();
    parameters.put("peelDirection", ParameterSchema.STRING);

    return Map.of(ACTIVITY_TYPE, parameters);
  }

  @Override
  public Optional<Activity<? extends StateContainer>> deserializeActivity(final SerializedActivity serializedActivity) {
    if (!serializedActivity.getTypeName().equals(ACTIVITY_TYPE)) {
      return Optional.empty();
    }

    final var activity = new PeelBananaActivity();

    for (final var entry : serializedActivity.getParameters().entrySet()) {
      final var value = entry.getValue();
      switch (entry.getKey()) {
        case "peelDirection": {
          if (value.isNull()) {
            activity.peelDirection = null;
          } else {
            activity.peelDirection = value.asString().orElseThrow(() -> new RuntimeException("Invalid parameter; expected string"));
          }
          break;
        }
        default:
          throw new RuntimeException("Unknown key `" + entry.getKey() + "`");
      }
    }

    return Optional.of(activity);
  }

  @Override
  public Optional<SerializedActivity> serializeActivity(final Activity<?> abstractActivity) {
    if (!(abstractActivity instanceof PeelBananaActivity)) {
      return Optional.empty();
    }

    final PeelBananaActivity activity = (PeelBananaActivity)abstractActivity;

    final var parameters = new HashMap<String, SerializedParameter>();
    parameters.put("peelDirection", (activity.peelDirection != null) ? SerializedParameter.of(activity.peelDirection) : SerializedParameter.NULL);

    return Optional.of(new SerializedActivity(ACTIVITY_TYPE, parameters));
  }
}
