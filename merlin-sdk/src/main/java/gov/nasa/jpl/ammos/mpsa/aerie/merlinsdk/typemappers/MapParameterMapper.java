package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class MapParameterMapper<K, V> implements ParameterMapper<Map<K, V>> {
  private final ParameterMapper<K> keyMapper;
  private final ParameterMapper<V> elementMapper;

  public MapParameterMapper(final ParameterMapper<K> keyMapper, final ParameterMapper<V> elementMapper) {
    this.keyMapper = keyMapper;
    this.elementMapper = elementMapper;
  }

  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.ofMap(Map.of(
        "keys", ParameterSchema.ofSequence(keyMapper.getParameterSchema()),
        "values", ParameterSchema.ofSequence(elementMapper.getParameterSchema())));
  }

  @Override
  public Result<Map<K, V>, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asMap()
        .map(Result::<Map<String, SerializedParameter>, String>success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedParameter.toString()))
        .match(
            serializedMap -> {
              final var keys = new ListParameterMapper<>(this.keyMapper)
                  .deserializeParameter(serializedMap.get("keys"))
                  .getSuccessOrThrow();
              final var values = new ListParameterMapper<>(this.elementMapper)
                  .deserializeParameter(serializedMap.get("values"))
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
  public SerializedParameter serializeParameter(final Map<K, V> fields) {
    final var keys = new ArrayList<K>();
    final var values = new ArrayList<V>();
    for (final var field : fields.entrySet()) {
      keys.add(field.getKey());
      values.add(field.getValue());
    }

    return SerializedParameter.of(Map.of(
        "keys", new ListParameterMapper<>(this.keyMapper).serializeParameter(keys),
        "values", new ListParameterMapper<>(this.elementMapper).serializeParameter(values)
    ));
  }
}
