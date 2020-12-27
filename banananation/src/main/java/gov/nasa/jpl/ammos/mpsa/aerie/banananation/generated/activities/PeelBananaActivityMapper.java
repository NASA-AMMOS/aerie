package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeelBananaActivityMapper implements ActivityMapper<PeelBananaActivity> {
  @Override
  public String getName() {
    return "PeelBanana";
  }

  @Override
  public Map<String, ValueSchema> getParameters() {
    return Map.of(
        "peelDirection", new StringValueMapper().getValueSchema());
  }

  @Override
  public PeelBananaActivity instantiateDefault() {
    return new PeelBananaActivity();
  }

  @Override
  public PeelBananaActivity instantiate(final Map<String, SerializedValue> arguments)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final var activity = new PeelBananaActivity();

    for (final var entry : arguments.entrySet()) {
      if ("peelDirection".equals(entry.getKey())) {
        activity.peelDirection = new StringValueMapper()
            .deserializeValue(entry.getValue())
            .getSuccessOrThrow($ -> new TaskSpecType.UnconstructableTaskSpecException());
      } else {
        throw new TaskSpecType.UnconstructableTaskSpecException();
      }
    }

    return activity;
  }

  @Override
  public Map<String, SerializedValue> getArguments(final PeelBananaActivity activity) {
    return Map.of(
        "peelDirection", new StringValueMapper().serializeValue(activity.peelDirection));
  }

  @Override
  public List<String> getValidationFailures(final PeelBananaActivity activity) {
    final var failures = new ArrayList<String>();
    if (!activity.validatePeelDirection()) failures.add("peel direction must be fromStem or fromTip");
    return failures;
  }
}
