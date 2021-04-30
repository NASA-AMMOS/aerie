package gov.nasa.jpl.aerie.merlin.protocol;

public interface AdaptationFactory {
  Adaptation<?> instantiate(final SerializedValue configuration);
}
