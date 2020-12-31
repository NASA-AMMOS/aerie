package gov.nasa.jpl.ammos.mpsa.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.function.Function;

public class DurationValueMapper implements ValueMapper<Duration> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.DURATION;
  }

  @Override
  public Result<Duration, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asInt()
        .map(v -> Duration.of(v, Duration.MICROSECONDS))
        .map((Function<Duration, Result<Duration, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected integer, got " + serializedValue.toString()));
  }

  @Override
  public SerializedValue serializeValue(final Duration value) {
    return SerializedValue.of(value.in(Duration.MICROSECONDS));
  }
}
