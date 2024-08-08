package gov.nasa.jpl.aerie.merlin.driver.timeline;

public interface EventSource {
  Cursor cursor();

  void freeze();

  interface Cursor {
    <State> Cell<State> stepUp(Cell<State> cell);
  }
}
