package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Optional;

public record OptionalValueMapper<T>(ValueMapper<T> elementMapper) implements ValueMapper<Optional<T>> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofSeries(elementMapper.getValueSchema());
  }

  @Override
  public Result<Optional<T>, String> deserializeValue(final SerializedValue serializedValue) {
    final var list = serializedValue.asList().get();
    if (list.isEmpty()) {
      return Result.success(Optional.empty());
    } else {
      return Result.success(Optional.of(elementMapper.deserializeValue(list.get(0)).getSuccessOrThrow()));
    }
  }

  @Override
  public SerializedValue serializeValue(final Optional<T> value) {
    return SerializedValue.of(value.stream().map(elementMapper::serializeValue).toList());
  }
}
