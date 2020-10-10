package gov.nasa.jpl.ammos.mpsa.aerie.contrib.typemappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.Result;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.List;
import java.util.Map;

public class Vector3DValueMapper implements ValueMapper<Vector3D> {
  @Override
  public ValueSchema getValueSchema() {
    return ValueSchema.ofStruct(Map.of(
        "x", ValueSchema.REAL,
        "y", ValueSchema.REAL,
        "z", ValueSchema.REAL));
  }

  @Override
  public Result<Vector3D, String> deserializeValue(final SerializedValue serializedValue) {
    return Result
        .from(serializedValue.asMap(), () -> "Expected list, got " + serializedValue.toString())
        .andThen(fields -> {
          final var mapper = new DoubleValueMapper();

          final var x$ = mapper.deserializeValue(fields.get("x"));
          final var y$ = mapper.deserializeValue(fields.get("y"));
          final var z$ = mapper.deserializeValue(fields.get("z"));

          return x$.par(y$).par(z$).mapSuccess(p -> new Vector3D(
              p.getLeft().getLeft(),
              p.getLeft().getRight(),
              p.getRight()));
        });
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
