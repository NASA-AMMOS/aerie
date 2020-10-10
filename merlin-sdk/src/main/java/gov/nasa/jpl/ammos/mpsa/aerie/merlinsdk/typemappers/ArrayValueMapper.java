package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ArrayValueMapper<T> implements ValueMapper<T[]> {
    private final ValueMapper<T> elementMapper;
    private final Class<? super T> elementClass;

    public ArrayValueMapper(final ValueMapper<T> elementMapper, Class<? super T> elementClass) {
        this.elementMapper = elementMapper;
        this.elementClass = elementClass;
    }

    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofSequence(elementMapper.getValueSchema());
    }

    @Override
    public Result<T[], String> deserializeValue(SerializedValue serializedValue) {
        return Result
            .from(serializedValue.asList(), () -> "Expected list, got " + serializedValue.toString())
            .andThen(serializedElements -> {
                @SuppressWarnings("unchecked")
                var elements$ = Result.<T[], String>success((T[]) Array.newInstance(this.elementClass, serializedElements.size()));

                for (int i = 0; i < serializedElements.size(); i += 1) {
                    final var idx = i;
                    final var element$ = this.elementMapper.deserializeValue(serializedElements.get(i));

                    elements$ = elements$.par(element$, (arr, element) -> {
                        arr[idx] = element;
                        return arr;
                    });
                }

                return elements$;
            });
    }

    @Override
    public SerializedValue serializeValue(T[] value) {
        final var serializedElements = new ArrayList<SerializedValue>();
        for (final var element : value) {
            serializedElements.add(this.elementMapper.serializeValue(element));
        }
        return SerializedValue.of(serializedElements);
    }
}
