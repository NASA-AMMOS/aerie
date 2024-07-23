package gov.nasa.jpl.aerie.contrib.serialization.mappers;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public record PairValueMapper<K, V>(ValueMapper<K> keyMapper, ValueMapper<V> valueMapper) implements ValueMapper<Pair<K, V>> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofStruct(Map.of("left", keyMapper.getValueSchema(), "right", valueMapper.getValueSchema()));
  }

  @Override
  public Result<Pair<K, V>, String> deserializeValue(final SerializedValue serializedValue) {
    final var map = serializedValue.asMap().get();
    return Result.success(Pair.of(
        keyMapper.deserializeValue(map.get("left")).getSuccessOrThrow(),
        valueMapper.deserializeValue(map.get("right")).getSuccessOrThrow()));
  }

  @Override
  public SerializedValue serializeValue(final Pair<K, V> value) {
    return SerializedValue.of(Map.of(
        "left", keyMapper.serializeValue(value.getLeft()),
        "right", valueMapper.serializeValue(value.getRight())
    ));
  }
}
