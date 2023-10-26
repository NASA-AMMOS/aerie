package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface Resource<D> {
  ErrorCatching<Expiring<D>> getDynamics();

  // By default, resources don't track their names
  default void registerName(String name) {}
}
