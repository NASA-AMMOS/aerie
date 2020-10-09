package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MapValueMapper<K, V> implements ValueMapper<Map<K, V>> {
  private final ValueMapper<K> keyMapper;
  private final ValueMapper<V> elementMapper;

  public MapValueMapper(final ValueMapper<K> keyMapper, final ValueMapper<V> elementMapper) {
    this.keyMapper = keyMapper;
    this.elementMapper = elementMapper;
  }

  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofSequence(ValueSchema.ofStruct(Map.of(
        "key", keyMapper.getValueSchema(),
        "value", elementMapper.getValueSchema())));
  }

  @Override
  public Result<Map<K, V>, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asList()
        .map((Function<List<SerializedValue>, Result<List<SerializedValue>, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
        .andThen(serializedList -> {
          var map$ = Result.<Map<K, V>, String>success(new HashMap<>());

          for (final SerializedValue element : serializedList) {
            final var elementSpec$ = element
                .asMap()
                .map((Function<Map<String, SerializedValue>, Result<Map<String, SerializedValue>, String>>) Result::success)
                .orElseGet(() -> Result.failure("Expected map, got " + element));

            map$ = map$.par(elementSpec$).andThen(uncurry(map -> elementSpec -> {
              final var key$ = keyMapper.deserializeValue(elementSpec.get("key"));
              final var value$ = elementMapper.deserializeValue(elementSpec.get("value"));

              return key$.par(value$).mapSuccess(uncurry((K key) -> (V value) -> {
                map.put(key, value);
                return map;
              })::apply);
            }));
          }

          return map$;
        });
  }

  @Override
  public SerializedValue serializeValue(final Map<K, V> fields) {
    final List<SerializedValue> elementSpecs = new ArrayList<>(fields.size());
    for (final var entry : fields.entrySet()) {
      elementSpecs.add(SerializedValue.of(
          Map.of(
              "key", keyMapper.serializeValue(entry.getKey()),
              "value", elementMapper.serializeValue(entry.getValue()))));
    }
    return SerializedValue.of(elementSpecs);
  }


  private static <Result, T1, T2>
  Function<Pair<T1, T2>, Result>
  uncurry(Function<T1, Function<T2, Result>> f) {
    return p -> f.apply(p.getLeft()).apply(p.getRight());
  }
}
