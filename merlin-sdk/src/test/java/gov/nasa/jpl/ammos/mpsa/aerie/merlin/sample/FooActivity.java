package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooActivity implements ActivityInstance {
  @Override
  public SerializedActivity serialize() {
    return new SerializedActivity("foo", Map.of());
  }

//  @Override
//  public List<String> getValidationFailures() {
//  }

  public static Map<String, ActivityType<FooActivity>> getActivityTypes() {
    return Map.of("foo", new ActivityType<>() {
      @Override
      public String getName() {
        return "foo";
      }

      @Override
      public Map<String, ValueSchema> getParameters() {
        return Map.of();
      }

      @Override
      public FooActivity instantiate(final Map<String, SerializedValue> arguments) {
        return new FooActivity();
      }
    });
  }
}
