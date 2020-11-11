package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooActivityInstance<$Schema> implements ActivityInstance {
  private final FooActivity<$Schema> activity = new FooActivity<>();

  @Override
  public SerializedActivity serialize() {
    return new SerializedActivity("foo", Map.of());
  }

//  @Override
//  public List<String> getValidationFailures() {
//  }

  public static <$Schema> Map<String, ActivityType<FooActivityInstance<$Schema>>> getActivityTypes() {
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
      public FooActivityInstance<$Schema> instantiate(final Map<String, SerializedValue> arguments) {
        return new FooActivityInstance<>();
      }
    });
  }
}
