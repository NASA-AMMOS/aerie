package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.function.Predicate;

public interface StateQuery<ResourceType> {
  ResourceType get();
  List<Window> when(Predicate<ResourceType> condition);
}
