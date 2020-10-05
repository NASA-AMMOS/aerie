package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;

public final class ListValueMapper<T> implements ValueMapper<List<T>> {
  private final ValueMapper<T> elementMapper;

  public ListValueMapper(final ValueMapper<T> elementMapper) {
    this.elementMapper = elementMapper;
  }

  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofSequence(elementMapper.getValueSchema());
  }

  @Override
  public Result<List<T>, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asList()
        .map(Result::<List<SerializedValue>, String>success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
        .match(
            serializedElements -> {
              final var elements = new ArrayList<T>();
              for (final var serializedElement : serializedElements) {
                final var result = this.elementMapper.deserializeValue(serializedElement);
                if (result.getKind() == Result.Kind.Failure) return result.mapSuccess(_left -> null);

                // SAFETY: `result` must be a Success variant.
                elements.add(result.getSuccessOrThrow());
              }
              return Result.success(elements);
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final List<T> elements) {
    final var serializedElements = new ArrayList<SerializedValue>();
    for (final var element : elements) {
      serializedElements.add(this.elementMapper.serializeValue(element));
    }
    return SerializedValue.of(serializedElements);
  }
}
