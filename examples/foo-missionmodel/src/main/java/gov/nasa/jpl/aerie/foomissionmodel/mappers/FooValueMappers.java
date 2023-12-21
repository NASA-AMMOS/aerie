package gov.nasa.jpl.aerie.foomissionmodel.mappers;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.Vector3DValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class FooValueMappers {
  public static ValueMapper<Vector3D> vector3d(final ValueMapper<Double> elementMapper) {
    return new Vector3DValueMapper(elementMapper);
  }
}
