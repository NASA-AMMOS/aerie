package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

public interface IncrementState<T> {
  void incrementBy(T value);
  void decrementBy(T value);

  T get();
}
