package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface Resource<D> extends ThinResource<ErrorCatching<Expiring<D>>> {
  // By default, resources don't track their names
  default void registerName(String name) {}
}
