package gov.nasa.jpl.aerie.fooadaptation.mappers;

import java.util.Map;
import java.util.function.Function;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.Vector3DValueMapper;
import gov.nasa.jpl.aerie.fooadaptation.Configuration;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.utilities.Result;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class FooValueMappers {
  public static ValueMapper<Vector3D> vector3d(final ValueMapper<Double> elementMapper) {
    return new Vector3DValueMapper(elementMapper);
  }

  public static ValueMapper<Configuration> configuration() {
    return new ValueMapper<>() {
      @Override
      public ValueSchema getValueSchema() {
        return ValueSchema.REAL;
      }

      @Override
      public Result<Configuration, String> deserializeValue(final SerializedValue serializedValue) {
        return serializedValue
            .asMap()
            .map(m -> m.get("sinkRate").asReal())
            .map(m -> new Configuration(m.get()))
            .map((Function<Configuration, Result<Configuration, String>>) Result::success)
            .orElseGet(() -> Result.failure("Expected string, got " + serializedValue.toString()));
      }

      @Override
      public SerializedValue serializeValue(final Configuration value) {
        return SerializedValue.of(Map.of("sinkRate", SerializedValue.of(value.sinkRate)));
      }
    };
  }
}
