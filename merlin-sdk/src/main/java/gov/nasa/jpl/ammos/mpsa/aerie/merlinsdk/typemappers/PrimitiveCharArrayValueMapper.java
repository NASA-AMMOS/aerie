package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PrimitiveCharArrayValueMapper implements ValueMapper<char[]> {
    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofSequence(ValueSchema.STRING);
    }

    @Override
    public Result<char[], String> deserializeValue(SerializedValue serializedValue) {
        var elementMapper = new CharacterValueMapper();
        return serializedValue
                .asList()
                .map((Function<List<SerializedValue>, Result<List<SerializedValue>, String>>) Result::success)
                .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
                .andThen(serializedElements -> {
                    var elements$ = Result.<char[], String>success(new char[serializedElements.size()]);
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
    public SerializedValue serializeValue(char[] elements) {
        final var serializedElements = new ArrayList<SerializedValue>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedValue.of(Character.toString(element)));
        }
        return SerializedValue.of(serializedElements);
    }
}
