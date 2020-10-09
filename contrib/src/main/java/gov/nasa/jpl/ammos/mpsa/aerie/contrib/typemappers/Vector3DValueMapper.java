package gov.nasa.jpl.ammos.mpsa.aerie.contrib.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.List;
import java.util.function.Function;

public class Vector3DValueMapper implements ValueMapper<Vector3D> {

  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofSequence(ValueSchema.REAL);
  }

  @Override
  public Result<Vector3D, String> deserializeValue(final SerializedValue serializedValue) {
    return serializedValue
        .asList()
        .map((Function<List<SerializedValue>, Result<List<SerializedValue>, String>>) Result::success)
        .orElseGet(() -> Result.failure("Expected list, got " + serializedValue.toString()))
        .match(
            serializedElements -> {
              if (serializedElements.size() != 3) return Result.failure("Expected 3 components, got " + serializedElements.size());
              final var components = new double[3];
              final var mapper = new DoubleValueMapper();
              for (int i=0; i<3; i++) {
                final var result = mapper.deserializeValue(serializedElements.get(i));
                if (result.getKind() == Result.Kind.Failure) return result.mapSuccess(_left -> null);

                // SAFETY: `result` must be a Success variant.
                components[i] = result.getSuccessOrThrow();
              }
              return Result.success(new Vector3D(components));
            },
            Result::failure
        );
  }

  @Override
  public SerializedValue serializeValue(final Vector3D value) {
    return SerializedValue.of(
        List.of(
            SerializedValue.of(value.getX()),
            SerializedValue.of(value.getY()),
            SerializedValue.of(value.getZ())
        )
    );
  }
}
