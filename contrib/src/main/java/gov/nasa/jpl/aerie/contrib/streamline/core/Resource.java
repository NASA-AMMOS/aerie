package gov.nasa.jpl.aerie.contrib.streamline.core;

/**
 * A function returning a fully-wrapped dynamics,
 * and the primary way models track state and report results.
 */
public interface Resource<D> extends ThinResource<ErrorCatching<Expiring<D>>> {
}
