package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PrimitiveBooleanArrayValueMapper implements ValueMapper<boolean[]> {
    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofSequence(ValueSchema.BOOLEAN);
    }

    @Override
    public Result<boolean[], String> deserializeValue(SerializedValue serializedValue) {
        var elementMapper = new BooleanValueMapper();
        return serializedValue
                .asList()
                .map((Function<List<SerializedValue>, Result<List<SerializedValue>, String>>) Result::success)
                .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
                .andThen(serializedElements -> {
                    var elements$ = Result.<boolean[], String>success(new boolean[serializedElements.size()]);
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
    public SerializedValue serializeValue(boolean[] elements) {
        final var serializedElements = new ArrayList<SerializedValue>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedValue.of(element));
        }
        return SerializedValue.of(serializedElements);
    }
}
