package gov.nasa.jpl.aerie.merlin.framework.dependency;

import java.util.List;

public interface Introspectable {
  List<Dependency> getDependencies(String methodName);
}
