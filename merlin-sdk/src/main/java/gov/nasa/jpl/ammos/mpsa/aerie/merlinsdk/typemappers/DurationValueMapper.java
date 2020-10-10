package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public class DurationValueMapper implements ValueMapper<Duration> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.DURATION;
  }

  @Override
  public Result<Duration, String> deserializeValue(final SerializedValue serializedValue) {
    return Result
        .from(serializedValue.asInt(), () -> "Expected integer, got " + serializedValue.toString())
        .mapSuccess(v -> Duration.of(v, Duration.MICROSECONDS));
  }

  @Override
  public SerializedValue serializeValue(final Duration value) {
    return SerializedValue.of(value.dividedBy(Duration.MICROSECOND));
  }
}
