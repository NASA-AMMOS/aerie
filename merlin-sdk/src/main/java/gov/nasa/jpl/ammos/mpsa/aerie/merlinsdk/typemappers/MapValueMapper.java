package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class MapValueMapper<K, V> implements ValueMapper<Map<K, V>> {
  private final ValueMapper<K> keyMapper;
  private final ValueMapper<V> elementMapper;

  public MapValueMapper(final ValueMapper<K> keyMapper, final ValueMapper<V> elementMapper) {
    this.keyMapper = keyMapper;
    this.elementMapper = elementMapper;
  }

  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.ofStruct(Map.of(
        "keys", ParameterSchema.ofSequence(keyMapper.getValueSchema()),
        "values", ParameterSchema.ofSequence(elementMapper.getValueSchema())));
  }

  @Override
  public Result<Map<K, V>, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asMap()
        .map(Result::<Map<String, SerializedValue>, String>success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
        .match(
            serializedMap -> {
              final var keys = new ListValueMapper<>(this.keyMapper)
                  .deserializeValue(serializedMap.get("keys"))
                  .getSuccessOrThrow();
              final var values = new ListValueMapper<>(this.elementMapper)
                  .deserializeValue(serializedMap.get("values"))
                  .getSuccessOrThrow();

              final var map = new HashMap<K, V>();
              for (var i = 0; i < keys.size(); i += 1) {
                map.put(keys.get(i), values.get(i));
              }

              return Result.success(map);
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final Map<K, V> fields) {
    final var keys = new ArrayList<K>();
    final var values = new ArrayList<V>();
    for (final var field : fields.entrySet()) {
      keys.add(field.getKey());
      values.add(field.getValue());
    }

    return SerializedValue.of(Map.of(
        "keys", new ListValueMapper<>(this.keyMapper).serializeValue(keys),
        "values", new ListValueMapper<>(this.elementMapper).serializeValue(values)
    ));
  }
}
