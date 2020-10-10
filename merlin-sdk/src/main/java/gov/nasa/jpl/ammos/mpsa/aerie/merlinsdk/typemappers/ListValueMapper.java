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
    return Result
        .from(serializedValue.asList(), () -> "Expected list, got " + serializedValue.toString())
        .andThen(serializedElements -> {
          var elements$ = Result.<List<T>, String>success(new ArrayList<>(serializedElements.size()));

          for (final var serializedElement : serializedElements) {
            final var element$ = this.elementMapper.deserializeValue(serializedElement);

            elements$ = elements$.par(element$, (elements, element) -> {
              elements.add(element);
              return elements;
            });
          }

          return elements$;
        });
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
