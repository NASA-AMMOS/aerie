package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ReplayingTask;
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

// TODO: Automatically generate at compile time.
/* package-local */
final class FooActivityTaskSpec {
  private FooActivityTaskSpec() {}

  public static <$Schema> TaskSpecType<$Schema, FooActivity> getDescriptor(
      final ProxyContext<$Schema> rootContext,
      final FooResources<$Schema> container)
  {
    return new TaskSpecType<>() {
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
      public FooActivity instantiateDefault() {
        return new FooActivity();
      }

      @Override
      public FooActivity instantiate(final Map<String, SerializedValue> arguments)
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

        return activity;
      }

      @Override
      public Map<String, SerializedValue> getArguments(final FooActivity activity) {
        return Map.of(
            "x", new IntegerValueMapper().serializeValue(activity.x),
            "y", new StringValueMapper().serializeValue(activity.y));
      }

      @Override
      public List<String> getValidationFailures(final FooActivity activity) {
        // TODO: Extract validation messages from @Validation annotation at compile time.
        final var failures = new ArrayList<String>();
        if (!activity.validateX()) failures.add("x cannot be exactly 99");
        if (!activity.validateY()) failures.add("y cannot be 'bad'");
        return failures;
      }

      @Override
      public <$Timeline extends $Schema>
      gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task<$Timeline>
      createTask(final FooActivity activity) {
        final var task = activity.new EffectModel<$Schema>();
        task.setContext(rootContext);
        return new ReplayingTask<>(rootContext, () -> task.run(container));
      }
    };
  }
}
