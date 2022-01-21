package gov.nasa.jpl.aerie.foomissionmodel.mappers;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.Vector3DValueMapper;
import gov.nasa.jpl.aerie.foomissionmodel.Configuration;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.Map;
import java.util.function.Function;

public class FooValueMappers {
  public static ValueMapper<Vector3D> vector3d(final ValueMapper<Double> elementMapper) {
    return new Vector3DValueMapper(elementMapper);
  }
}
