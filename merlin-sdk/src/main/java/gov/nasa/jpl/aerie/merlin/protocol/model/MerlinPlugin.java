package gov.nasa.jpl.aerie.merlin.protocol.model;

/** A service interface suitable for use with {@link java.util.ServiceLoader}. */
public interface MerlinPlugin {
  MissionModelFactory<?, ?> getFactory();
}
