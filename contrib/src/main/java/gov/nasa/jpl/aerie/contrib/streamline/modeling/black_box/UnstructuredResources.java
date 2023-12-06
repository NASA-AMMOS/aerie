package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;

public final class UnstructuredResources {
  private UnstructuredResources() {}

  public static <A> Resource<Unstructured<A>> constant(A value) {
    var result = UnstructuredResourceApplicative.pure(value);
    name(result, value.toString());
    return result;
  }

  public static <A> Resource<Unstructured<A>> timeBased(Function<Duration, A> f) {
    // Put this in a cell so it'll be stepped up appropriately
    return cellResource(Unstructured.timeBased(f));
  }
  
  public static <A, D extends Dynamics<A, D>> Resource<Unstructured<A>> asUnstructured(Resource<D> resource) {
    return map(resource, Unstructured::unstructured);
  }
}
