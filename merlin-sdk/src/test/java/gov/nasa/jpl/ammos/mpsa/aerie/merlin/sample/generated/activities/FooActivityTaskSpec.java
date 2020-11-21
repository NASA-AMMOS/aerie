package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities.FooActivity;
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
final class FooActivityTaskSpec extends TaskSpec {
  private final FooActivity activity;

  private FooActivityTaskSpec(final FooActivity activity) {
    this.activity = Objects.requireNonNull(activity);
  }

  @Override
  public String getTypeName() {
    return "foo";
  }

  @Override
  public Map<String, SerializedValue> getArguments() {
    return Map.of(
        "x", new IntegerValueMapper().serializeValue(this.activity.x),
        "y", new StringValueMapper().serializeValue(this.activity.y));
  }

  @Override
  public <$Schema> Task<$Schema, FooResources<$Schema>> createTask() {
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

  public static final TaskSpecType<TaskSpec> descriptor = new TaskSpecType<>() {
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
    public TaskSpec instantiateDefault() {
      return new FooActivityTaskSpec(new FooActivity());
    }

    @Override
    public TaskSpec instantiate(final Map<String, SerializedValue> arguments)
    throws UnconstructableTaskSpecException
    {
      final var activity = new FooActivity();

      for (final var entry : arguments.entrySet()) {
        switch (entry.getKey()) {
          case "x":
            activity.x = new IntegerValueMapper()
                .deserializeValue(entry.getValue())
                .getSuccessOrThrow($ -> new UnconstructableTaskSpecException());
            break;

          case "y":
            activity.y = new StringValueMapper()
                .deserializeValue(entry.getValue())
                .getSuccessOrThrow($ -> new UnconstructableTaskSpecException());
            break;

          default:
            throw new UnconstructableTaskSpecException();
        }
      }

      return new FooActivityTaskSpec(activity);
    }
  };
}
