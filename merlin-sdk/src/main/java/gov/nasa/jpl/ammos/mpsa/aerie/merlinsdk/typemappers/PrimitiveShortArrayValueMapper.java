package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;

public class PrimitiveShortArrayValueMapper implements ValueMapper<short[]> {
    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofSequence(ValueSchema.INT);
    }

    @Override
    public Result<short[], String> deserializeValue(SerializedValue serializedValue) {
        var elementMapper = new ShortValueMapper();
        return Result
                .from(serializedValue.asList(), () -> "Expected list, got " + serializedValue.toString())
                .andThen(serializedElements -> {
                    var elements$ = Result.<short[], String>success(new short[serializedElements.size()]);
                    for (int i = 0; i < serializedElements.size(); i += 1) {
                        final var idx = i;
                        final var result$ = elementMapper.deserializeValue(serializedElements.get(i));

                        elements$ = elements$.par(result$, (components, result) -> {
                            components[idx] = result;
                            return components;
                        });
                    }
                    return elements$;
                });
    }

    @Override
    public SerializedValue serializeValue(short[] elements) {
        final var serializedElements = new ArrayList<SerializedValue>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedValue.of(element));
        }
        return SerializedValue.of(serializedElements);
    }
}
