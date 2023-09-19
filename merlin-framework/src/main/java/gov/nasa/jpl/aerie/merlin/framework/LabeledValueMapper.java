package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public record LabeledValueMapper<T>(String label, ValueMapper<T> target) implements ValueMapper<T> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.withLabel(label, target.getValueSchema());
  }

  @Override
  public Result<T, String> deserializeValue(final SerializedValue serializedValue) {
    return target.deserializeValue(serializedValue);
  }

  @Override
  public SerializedValue serializeValue(final T value) {
    return target.serializeValue(value);
  }
}
