package gov.nasa.jpl.aerie.contrib.streamline.core;

public interface DynamicsEffect<D extends Dynamics<?, D>> {
    ErrorCatching<Expiring<D>> apply(ErrorCatching<Expiring<D>> dynamics);
}
