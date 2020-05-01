package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveBooleanArrayParameterMapper implements ParameterMapper<boolean[]> {
    @Override
    public ParameterSchema getParameterSchema() {
        return ParameterSchema.ofList(ParameterSchema.BOOLEAN);
    }

    @Override
    public Result<boolean[], String> deserializeParameter(SerializedParameter serializedParameter) {
        var elementMapper = new BooleanParameterMapper();
        return serializedParameter
                .asList()
                .map(Result::<List<SerializedParameter>, String>success)
                .orElseGet(() -> Result.failure("Expected list, got " + serializedParameter.toString()))
                .match(
                        serializedElements -> {
                            final boolean[] elements = new boolean[serializedElements.size()];
                            int index = 0;
                            for (final var serializedElement : serializedElements) {
                                final var result = elementMapper.deserializeParameter(serializedElement);
                                if (result.getKind() == Result.Kind.Failure) return result.mapSuccess(_left -> null);

                                // SAFETY: `result` must be a Success variant.
                                elements[index++] = result.getSuccessOrThrow();
                            }
                            return Result.success(elements);
                        },
                        Result::failure
                );
    }

    @Override
    public SerializedParameter serializeParameter(boolean[] elements) {
        final var serializedElements = new ArrayList<SerializedParameter>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedParameter.of(element));
        }
        return SerializedParameter.of(serializedElements);
    }
}
