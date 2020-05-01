package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveDoubleArrayParameterMapper implements ParameterMapper<double[]> {
    @Override
    public ParameterSchema getParameterSchema() {
        return ParameterSchema.ofList(ParameterSchema.REAL);
    }

    @Override
    public Result<double[], String> deserializeParameter(SerializedParameter serializedParameter) {
        var elementMapper = new DoubleParameterMapper();
        return serializedParameter
                .asList()
                .map(Result::<List<SerializedParameter>, String>success)
                .orElseGet(() -> Result.failure("Expected list, got " + serializedParameter.toString()))
                .match(
                        serializedElements -> {
                            final double[] elements = new double[serializedElements.size()];
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
    public SerializedParameter serializeParameter(double[] elements) {
        final var serializedElements = new ArrayList<SerializedParameter>(elements.length);
        for (final var element : elements) {
            serializedElements.add(SerializedParameter.of(element));
        }
        return SerializedParameter.of(serializedElements);
    }
}