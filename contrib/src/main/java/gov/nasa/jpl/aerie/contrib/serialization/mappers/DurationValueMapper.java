package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.function.Function;

public final class DurationValueMapper implements ValueMapper<Duration> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.STRING;
  }

  @Override
  public Result<Duration, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asString()
        .map(Long::parseLong)
        .map(v -> Duration.of(v, Duration.MICROSECONDS))
        .map((Function<Duration, Result<Duration, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected duration int wrapped in string, got " + serializedValue.toString()));
  }

  @Override
  public SerializedValue serializeValue(final Duration value) {
    return SerializedValue.of(Long.toString(value.in(Duration.MICROSECONDS)));
  }
}
