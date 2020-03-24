package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;

public final class NullableParameterMapper<T> implements ParameterMapper<T> {
  private final ParameterMapper<T> valueMapper;

  public NullableParameterMapper(final ParameterMapper<T> valueMapper) {
    this.valueMapper = valueMapper;
  }

  @Override
  public ParameterSchema getParameterSchema() {
    return this.valueMapper.getParameterSchema();
  }

  @Override
  public Result<T, String> deserializeParameter(final SerializedParameter serializedParameter) {
    if (serializedParameter.isNull()) {
      return Result.success(null);
    } else {
      return this.valueMapper.deserializeParameter(serializedParameter);
    }
  }

  @Override
  public SerializedParameter serializeParameter(final T parameter) {
    if (parameter == null) {
      return SerializedParameter.NULL;
    } else {
      return this.valueMapper.serializeParameter(parameter);
    }
  }
}
