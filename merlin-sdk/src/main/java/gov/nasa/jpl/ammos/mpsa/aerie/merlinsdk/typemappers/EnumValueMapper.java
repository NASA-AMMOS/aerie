package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.util.function.Function;

public final class EnumValueMapper<E extends Enum<E>> implements ValueMapper<E> {
    private final Class<E> enumType;

    public EnumValueMapper(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.ofEnum(enumType);
    }

    @Override
    public Result<E, String> deserializeValue(SerializedValue serializedValue) {
        return Result
            .from(serializedValue.asString(), () -> "Expected string, got: ")
            .andThen(this::deserializeEnumValue);
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
