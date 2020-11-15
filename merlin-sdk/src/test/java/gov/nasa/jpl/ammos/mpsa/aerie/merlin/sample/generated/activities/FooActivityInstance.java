package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// TODO: Automatically generate at compile time.
/* package-local */
final class FooActivityInstance extends ActivityInstance {
  private final FooActivity activity;

  private FooActivityInstance(final FooActivity activity) {
    this.activity = Objects.requireNonNull(activity);
  }

  @Override
  public SerializedActivity serialize() {
    return new SerializedActivity("foo", Map.of(
        "x", new IntegerValueMapper().serializeValue(this.activity.x),
        "y", new StringValueMapper().serializeValue(this.activity.y)));
  }

  @Override
  public <$Schema> Task<$Schema, FooEvent, ActivityInstance, FooResources<$Schema>> createTask() {
    return this.activity.new EffectModel<>();
  }

  @Override
  public List<String> getValidationFailures() {
    // TODO: Extract validation messages from @Validation annotation at compile time.
    final var failures = new ArrayList<String>();
    if (!this.activity.validateX()) failures.add("x cannot be exactly 99");
    if (!this.activity.validateY()) failures.add("y cannot be 'bad'");
    return failures;
  }

  public static final ActivityType<ActivityInstance> descriptor = new ActivityType<>() {
    @Override
    public String getName() {
      return "foo";
    }

    @Override
    public Map<String, ValueSchema> getParameters() {
      return Map.of(
          "x", new IntegerValueMapper().getValueSchema(),
          "y", new StringValueMapper().getValueSchema());
    }

    @Override
    public ActivityInstance instantiate(final Map<String, SerializedValue> arguments)
    throws UnconstructableActivityException
    {
      final var activity = new FooActivity();

      for (final var entry : arguments.entrySet()) {
        switch (entry.getKey()) {
          case "x":
            activity.x = new IntegerValueMapper()
                .deserializeValue(entry.getValue())
                .getSuccessOrThrow($ -> new UnconstructableActivityException());
            break;

          case "y":
            activity.y = new StringValueMapper()
                .deserializeValue(entry.getValue())
                .getSuccessOrThrow($ -> new UnconstructableActivityException());
            break;

          default:
            throw new UnconstructableActivityException();
        }
      }

      return new FooActivityInstance(activity);
    }
  };
}
