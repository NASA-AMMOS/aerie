package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class EnumParameterMapper<E extends Enum<E>> implements ParameterMapper<E> {
    private final Class<E> enumType;

    public EnumParameterMapper(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public ParameterSchema getParameterSchema() {
        return ParameterSchema.ofEnum(enumType);
    }

    @Override
    public Result<E, String> deserializeParameter(SerializedParameter serializedParameter) {
        return serializedParameter
                .asString()
                .map(Result::<String, String>success)
                .orElseGet(() -> Result.failure("Expected string, got: "))
                .match(this::deserializeEnumValue, Result::failure);
    }

    @Override
    public SerializedParameter serializeParameter(E parameter) {
        return SerializedParameter.of(parameter.name());
    }

    private Result<E, String> deserializeEnumValue(String name) {
        try {
            return Result.success(Enum.valueOf(enumType, name));
        } catch (IllegalArgumentException e) {
            return Result.failure(String.format("%s is not a valid value for enum %s", name, enumType.getCanonicalName()));
        }
    }
}
