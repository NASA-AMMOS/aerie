package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooActivityInstance implements ActivityInstance {
  private final FooActivity activity = new FooActivity();

  @Override
  public SerializedActivity serialize() {
    return new SerializedActivity("foo", Map.of());
  }

  public <$Schema> void run(
      final Context<? extends $Schema, FooEvent, FooActivityInstance> ctx,
      final FooResources<$Schema> resources)
  {
    this.activity.new EffectModel<$Schema>().modelEffects(ctx, resources);
  }

//  @Override
//  public List<String> getValidationFailures() {
//  }

  public static Map<String, ActivityType<FooActivityInstance>> getActivityTypes() {
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
      public FooActivityInstance instantiate(final Map<String, SerializedValue> arguments) {
        return new FooActivityInstance();
      }
    });
  }
}
