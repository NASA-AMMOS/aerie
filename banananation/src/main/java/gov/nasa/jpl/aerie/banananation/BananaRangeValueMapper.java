package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;
import java.util.function.Function;

public final class BananaRangeValueMapper<T extends Comparable<T>> implements ValueMapper<BananaRange<T>> {
  private final ValueMapper<T> elementMapper;

  public BananaRangeValueMapper(final ValueMapper<T> elementMapper) {
    this.elementMapper = elementMapper;
  }

  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofStruct(Map.of(
        "a", elementMapper.getValueSchema(),
        "b", elementMapper.getValueSchema()
     ));
  }

  @Override
  public Result<BananaRange<T>, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asMap()
        .map((Function<Map<String, SerializedValue>, Result<Map<String, SerializedValue>, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected map, got " + serializedValue.toString()))
        .match(
            serializedElements -> {
              final var a = this.elementMapper.deserializeValue(serializedElements.get("a")).getSuccessOrThrow();
              final var b = this.elementMapper.deserializeValue(serializedElements.get("b")).getSuccessOrThrow();
              return Result.success(new BananaRange<>(a, b));
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final BananaRange<T> elements) {
    return SerializedValue.of(Map.of(
        "a", this.elementMapper.serializeValue(elements.a()),
        "b", this.elementMapper.serializeValue(elements.b())
    ));
  }
}
