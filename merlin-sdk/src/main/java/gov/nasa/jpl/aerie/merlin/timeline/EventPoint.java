package gov.nasa.jpl.aerie.merlin.timeline;

import java.util.List;
import java.util.Objects;

/**
 * The class of points in time and their observable past.
 */
/* package-local */
abstract class EventPoint {
  private EventPoint() {}

  public abstract int getPrevious();
  public abstract List<Long> getCounts();

  /**
   * A time point advancing from a prior time point by a discontinuous event.
   */
  public static final class Advancing extends EventPoint {
    public final int previous;
    public final int tableIndex;
    public final int eventIndex;
    public final List<Long> counts;

    public Advancing(final int previous, final int tableIndex, final int eventIndex, final List<Long> counts) {
      this.previous = previous;
      this.tableIndex = tableIndex;
      this.eventIndex = eventIndex;
      this.counts = Objects.requireNonNull(counts);
    }

    @Override
    public int getPrevious() {
      return this.previous;
    }

    @Override
    public List<Long> getCounts() {
      return this.counts;
    }
  }

  /**
   * A time point advancing from a prior time point by waiting for a duration of time.
   */
  public static final class Waiting extends EventPoint {
    public final int previous;
    public final long microseconds;
    public final List<Long> counts;

    public Waiting(final int previous, final long microseconds, final List<Long> counts) {
      this.previous = previous;
      this.microseconds = microseconds;
      this.counts = Objects.requireNonNull(counts);
    }

    @Override
    public int getPrevious() {
      return this.previous;
    }

    @Override
    public List<Long> getCounts() {
      return this.counts;
    }
  }

  /**
   * A time point that observes two parallel timelines forked from a common base point.
   */
  public static final class Joining extends EventPoint {
    public final int base;
    public final int left;
    public final int right;
    public final List<Long> counts;

    public Joining(final int base, final int left, final int right, final List<Long> counts) {
      this.base = base;
      this.left = left;
      this.right = right;
      this.counts = Objects.requireNonNull(counts);
    }

    @Override
    public int getPrevious() {
      return this.base;
    }

    @Override
    public List<Long> getCounts() {
      return this.counts;
    }
  }
}
