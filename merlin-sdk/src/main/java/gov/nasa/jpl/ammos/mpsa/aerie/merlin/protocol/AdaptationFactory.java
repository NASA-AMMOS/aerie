package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

public interface AdaptationFactory {
  Adaptation<?, ?> instantiate();
}
