package gov.nasa.jpl.aerie.merlin.driver.retracing.timeline;

public final class LiveCell<State> {
  private final Cell<State> cell;
  private final EventSource.Cursor cursor;

  public LiveCell(final Cell<State> cell, final EventSource.Cursor cursor) {
    this.cell = cell;
    this.cursor = cursor;
  }

  public Cell<State> get() {
    this.cursor.stepUp(this.cell);
    return this.cell;
  }
}
