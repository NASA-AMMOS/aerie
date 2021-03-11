package gov.nasa.jpl.aerie.merlin.protocol;

import java.util.Map;

// Every type of Resource has a unique type of dynamics.
public interface ResourceFamily<$Schema, Resource> {
  Map<String, Resource> getResources();

  ResourceSolver<$Schema, Resource, ?> getSolver();
}
