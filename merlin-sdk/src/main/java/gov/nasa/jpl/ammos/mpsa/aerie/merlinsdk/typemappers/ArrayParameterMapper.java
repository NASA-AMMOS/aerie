package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayParameterMapper<T> implements ParameterMapper<T[]> {
    private final ParameterMapper<T> elementMapper;
    private final Class<T> elementClass;

    public ArrayParameterMapper(final ParameterMapper<T> elementMapper, Class<T> elementClass) {
        this.elementMapper = elementMapper;
        this.elementClass = elementClass;
    }

    @Override
    public ParameterSchema getParameterSchema() {
        return ParameterSchema.ofList(elementMapper.getParameterSchema());
    }

    @Override
    public Result<T[], String> deserializeParameter(SerializedParameter serializedParameter) {
        final List<SerializedParameter> list = serializedParameter.asList().get(); // TODO: Could fail, should fix
        final T[] arr = (T[])Array.newInstance(this.elementClass, list.size());
        for (int i=0; i<list.size(); i++) {
            arr[i] = this.elementMapper.deserializeParameter(list.get(i)).getSuccessOrThrow();
        }
        return Result.success(arr);
    }

    @Override
    public SerializedParameter serializeParameter(T[] parameter) {
        final var serializedElements = new ArrayList<SerializedParameter>();
        for (final var element : parameter) {
            serializedElements.add(this.elementMapper.serializeParameter(element));
        }
        return SerializedParameter.of(serializedElements);
    }
}
