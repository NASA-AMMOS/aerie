package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;

public final class ListValueMapper<T> implements ValueMapper<List<T>> {
  private final ValueMapper<T> elementMapper;

  public ListValueMapper(final ValueMapper<T> elementMapper) {
    this.elementMapper = elementMapper;
  }

  @Override
  public ParameterSchema getValueSchema() {
    return ParameterSchema.ofSequence(elementMapper.getValueSchema());
  }

  @Override
  public Result<List<T>, String> deserializeValue(final SerializedParameter serializedValue) {
    return serializedValue
        .asList()
        .map(Result::<List<SerializedParameter>, String>success)
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
  public SerializedParameter serializeValue(final List<T> elements) {
    final var serializedElements = new ArrayList<SerializedParameter>();
    for (final var element : elements) {
      serializedElements.add(this.elementMapper.serializeValue(element));
    }
    return SerializedParameter.of(serializedElements);
  }
}
