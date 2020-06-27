package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline;

/**
 * The class of points in time and their observable past.
 *
 * @param <Event> The type of events that may be observed.
 */
/* package-local */
abstract class EventPoint<Event> {
  private EventPoint() {}

  abstract int getPrevious();

  /**
   * A time point advancing from a prior time point by a discontinuous event.
   *
   * @param <Event> The type of event.
   */
  /* package-local */
  static final class Advancing<Event> extends EventPoint<Event> {
    public final int previous;
    public final Event event;

    public Advancing(final int previous, final Event event) {
      this.previous = previous;
      this.event = event;
    }

    @Override
    int getPrevious() {
      return this.previous;
    }
  }

  /**
   * A time point advancing from a prior time point by waiting for a duration of time.
   *
   * @param <Event> The type of event observable in the past.
   */
  /* package-local */
  public static final class Waiting<Event> extends EventPoint<Event> {
    public final int previous;
    public final long microseconds;

    public Waiting(final int previous, final long microseconds) {
      this.previous = previous;
      this.microseconds = microseconds;
    }

    @Override
    int getPrevious() {
      return this.previous;
    }
  }

  /**
   * A time point that observes two parallel timelines forked from a common base point.
   *
   * @param <Event> The type of event observable in the past.
   */
  /* package-local */
  public static final class Joining<Event> extends EventPoint<Event> {
    public final int base;
    public final int left;
    public final int right;

    public Joining(final int base, final int left, final int right) {
      this.base = base;
      this.left = left;
      this.right = right;
    }

    @Override
    int getPrevious() {
      return this.base;
    }
  }
}
