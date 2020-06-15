package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.states;

public interface SettableCumulableResource<T, Delta>
    extends GettableResource<T>,
    CumulableResource<Delta>
{}
