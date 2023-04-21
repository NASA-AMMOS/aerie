package gov.nasa.jpl.aerie.merlin.driver.timeline;

public final class LiveCell<State> {
  private final Cell<State> cell;
  public final EventSource.Cursor cursor;

  public LiveCell(final Cell<State> cell, final EventSource.Cursor cursor) {
    this.cell = cell;
    this.cursor = cursor;
  }

  public Cell<State> get() {
    // this.cursor.stepUp(this.cell);   // commenting out; how far to step a cell now requires context; should probably get rid of LiveCell class since cursor isn't useful here anymore
    return this.cell;
  }
}
