package gov.nasa.jpl.aerie.command_model.value_mappers;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

/**
 * Fake value mapper to let us use Runnable in {@link gov.nasa.jpl.aerie.command_model.activities.CommandSpan}
 */
public class RunnableValueMapper implements ValueMapper<Runnable> {
    @Override
    public ValueSchema getValueSchema() {
        return ValueSchema.STRING;
    }

    @Override
    public Result<Runnable, String> deserializeValue(SerializedValue serializedValue) {
        return Result.success(() -> {});
    }

    @Override
    public SerializedValue serializeValue(Runnable value) {
        return SerializedValue.of("");
    }
}
