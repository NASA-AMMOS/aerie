package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ArrayValueMapper<T> implements ValueMapper<T[]> {
    private final ValueMapper<T> elementMapper;
    private final Class<? super T> elementClass;

    public ArrayValueMapper(final ValueMapper<T> elementMapper, Class<? super T> elementClass) {
        this.elementMapper = elementMapper;
        this.elementClass = elementClass;
    }

    @Override
    public ParameterSchema getValueSchema() {
        return ParameterSchema.ofSequence(elementMapper.getValueSchema());
    }

    @Override
    public Result<T[], String> deserializeValue(SerializedParameter serializedValue) {
        final var list = serializedValue.asList().get(); // TODO: Could fail, should fix

        @SuppressWarnings("unchecked")
        final var arr = (T[]) Array.newInstance(this.elementClass, list.size());

        for (int i=0; i<list.size(); i++) {
            arr[i] = this.elementMapper.deserializeValue(list.get(i)).getSuccessOrThrow();
        }

        return Result.success(arr);
    }

    @Override
    public SerializedParameter serializeValue(T[] value) {
        final var serializedElements = new ArrayList<SerializedParameter>();
        for (final var element : value) {
            serializedElements.add(this.elementMapper.serializeValue(element));
        }
        return SerializedParameter.of(serializedElements);
    }
}
