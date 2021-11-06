package gov.nasa.jpl.aerie.merlin.driver.timeline;

public interface EventSource {
  Cursor cursor();

  interface Cursor {
    void stepUp(Cell<?> cell);
  }
}
