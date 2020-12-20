package gov.nasa.jpl.ammos.mpsa.aerie.banananation2.generated.mappers;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation2.activities.BiteBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BiteBananaActivityMapper {
  public String getName() {
    return "BiteBanana";
  }

  public Map<String, ValueSchema> getParameters() {
    return Map.of(
        "biteSize", new DoubleValueMapper().getValueSchema());
  }

  public BiteBananaActivity instantiateDefault() {
    return new BiteBananaActivity();
  }

  public BiteBananaActivity instantiate(final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final var activity = new BiteBananaActivity();

    for (final var entry : arguments.entrySet()) {
      if ("biteSize".equals(entry.getKey())) {
        activity.biteSize = new DoubleValueMapper()
            .deserializeValue(entry.getValue())
            .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
      } else {
        throw new TaskSpecType.UnconstructableTaskSpecException();
      }
    }

    return activity;
  }

  public Map<String, SerializedValue> getArguments(final BiteBananaActivity activity) {
    return Map.of(
        "biteSize", new DoubleValueMapper().serializeValue(activity.biteSize));
  }

  public List<String> getValidationFailures(final BiteBananaActivity activity) {
    // TODO: Extract validation messages from @Validation annotation at compile time.
    final var failures = new ArrayList<String>();
    if (!activity.validateBiteSize()) failures.add("bite size must be positive");
    return failures;
  }
}
