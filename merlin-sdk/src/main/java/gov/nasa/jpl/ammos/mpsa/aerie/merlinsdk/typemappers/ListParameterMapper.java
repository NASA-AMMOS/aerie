package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;


public final class ListParameterMapper<T> implements ParameterMapper<List<T>> {
  private final ParameterMapper<T> elementMapper;

  public ListParameterMapper(final ParameterMapper<T> elementMapper) {
    this.elementMapper = elementMapper;
  }

  @Override
  public ParameterSchema getParameterSchema() {
    return ParameterSchema.ofList(elementMapper.getParameterSchema());
  }

  @Override
  public Result<List<T>, String> deserializeParameter(final SerializedParameter serializedParameter) {
    return serializedParameter
        .asList()
        .map(Result::<List<SerializedParameter>, String>success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedParameter.toString()))
        .match(
            serializedElements -> {
              final var elements = new ArrayList<T>();
              for (final var serializedElement : serializedElements) {
                final var result = this.elementMapper.deserializeParameter(serializedElement);
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
  public SerializedParameter serializeParameter(final List<T> elements) {
    final var serializedElements = new ArrayList<SerializedParameter>();
    for (final var element : elements) {
      serializedElements.add(this.elementMapper.serializeParameter(element));
    }
    return SerializedParameter.of(serializedElements);
  }
}
