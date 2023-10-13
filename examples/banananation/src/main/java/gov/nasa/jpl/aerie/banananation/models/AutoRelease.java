package gov.nasa.jpl.aerie.banananation.models;

public interface AutoRelease extends AutoCloseable {
  @Override
  void close();
}
