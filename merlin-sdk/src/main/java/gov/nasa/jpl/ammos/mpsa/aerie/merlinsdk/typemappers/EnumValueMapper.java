package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class EnumValueMapper<E extends Enum<E>> implements ValueMapper<E> {
    private final Class<E> enumType;

    public EnumValueMapper(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public ParameterSchema getValueSchema() {
        return ParameterSchema.ofEnum(enumType);
    }

    @Override
    public Result<E, String> deserializeValue(SerializedValue serializedValue) {
        return serializedValue
                .asString()
                .map(Result::<String, String>success)
                .orElseGet(() -> Result.failure("Expected string, got: "))
                .match(this::deserializeEnumValue, Result::failure);
    }

    @Override
    public SerializedValue serializeValue(E value) {
        return SerializedValue.of(value.name());
    }

    private Result<E, String> deserializeEnumValue(String name) {
        try {
            return Result.success(Enum.valueOf(enumType, name));
        } catch (IllegalArgumentException e) {
            return Result.failure(String.format("%s is not a valid value for enum %s", name, enumType.getCanonicalName()));
        }
    }
}
