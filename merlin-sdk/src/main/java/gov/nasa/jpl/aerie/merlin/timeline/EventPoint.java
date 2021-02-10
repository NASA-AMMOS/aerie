package gov.nasa.jpl.aerie.merlin.timeline;

/**
 * The class of points in time and their observable past.
 */
/* package-local */
abstract class EventPoint {
  private EventPoint() {}

  public abstract int getPrevious();

  /**
   * A time point advancing from a prior time point by a discontinuous event.
   */
  public static final class Advancing extends EventPoint {
    public final int previous;
    public final int tableIndex;
    public final int eventIndex;

    public Advancing(final int previous, final int tableIndex, final int eventIndex) {
      this.previous = previous;
      this.tableIndex = tableIndex;
      this.eventIndex = eventIndex;
    }

    @Override
    public int getPrevious() {
      return this.previous;
    }
  }

  /**
   * A time point advancing from a prior time point by waiting for a duration of time.
   */
  public static final class Waiting extends EventPoint {
    public final int previous;
    public final long microseconds;

    public Waiting(final int previous, final long microseconds) {
      this.previous = previous;
      this.microseconds = microseconds;
    }

    @Override
    public int getPrevious() {
      return this.previous;
    }
  }

  /**
   * A time point that observes two parallel timelines forked from a common base point.
   */
  public static final class Joining extends EventPoint {
    public final int base;
    public final int left;
    public final int right;

    public Joining(final int base, final int left, final int right) {
      this.base = base;
      this.left = left;
      this.right = right;
    }

    @Override
    public int getPrevious() {
      return this.base;
    }
  }
}
