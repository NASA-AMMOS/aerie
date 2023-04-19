package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This is a TemporalEventSource that is a modification to another TemporalEventSource that replaces or adds EventGraphs
 * at Timepoints.  The original TemporalEventSource is used except where graphs are replaced.
 */
public class TemporalEventSourceDelta extends TemporalEventSource {
  //final private TreeMap<Duration, EventGraph<Event>> modifications = new TreeMap<>();
  final private TemporalEventSource oldTemporalEventSource;

  public TemporalEventSourceDelta(TemporalEventSource oldTemporalEventSource) {
    this.oldTemporalEventSource = oldTemporalEventSource;
  }

  @Override
  public void add(final Duration delta) {
    // TODO: REVIEW - Should this even be allowed?
    //super.add(delta);
  }

  /**
   * Add the graph to the {@code points} list at a specified time.
   * The oldTemporalEventSource is never modified
   * @param graph the graph to add
   * @param time the time of the graph
   */
  @Override
  public void add(final EventGraph<Event> graph, final Duration time) {
//    modifications.put(time, graph);
    super.addIndices(graph, time, null);
  }

  @Override
  public TemporalCursor cursor() {
    return new TemporalCursor(this.iterator());
  }

  /**
   * Iterate over TimePoints in points but include additions and replacements (in {@code this.modifications})
   * @return an iterator over TimePoints
   */
  @Override
  public Iterator<TemporalEventSource.TimePoint> iterator() {
    return new Iterator<>() {
      private Iterator<TemporalEventSource.TimePoint> oldIter = oldEventSource.iterator();
      private Duration accumulatedDuration = Duration.ZERO;
      private Duration lastTime = Duration.ZERO;
      private TemporalEventSource.TimePoint peek = null;
      private Iterator<Map.Entry<Duration, EventGraph<Event>>> riter =
          TemporalEventSourceDelta.this.eventsByTime.entrySet().iterator();
      private  Map.Entry<Duration, EventGraph<Event>> rpeek = null;

      @Override
      public boolean hasNext() {
        if (peek != null) return true;
        if (rpeek != null) return true;
        if (oldIter.hasNext()) return true;
        if (riter.hasNext()) return true;
        return false;
      }

      @Override
      public TemporalEventSource.TimePoint next() {
        // TODO: This essentially builds a new list of TimePoints like this.points.
        //       If we're going to use this iterator a lot, then should save and reuse it?
        //       May need to check for staleness.

        // Get next peek and rpeek values if null, calling iter.next() and riter.next()
        if (peek == null && oldIter.hasNext()) {
          peek = oldIter.next();
          if (peek instanceof TimePoint.Delta d) {
            accumulatedDuration = d.delta().plus(accumulatedDuration);
          }
        }
        if (rpeek == null && riter.hasNext()) {
          rpeek = riter.next();
        }
        // If we didn't get anything, then we have no elements and throw an exception
        if (peek == null && rpeek == null) {
          if (oldIter.hasNext() || riter.hasNext()) throw new AssertionError();
          throw new NoSuchElementException();
        }

        // Determine if the replacement or original TimePoint is next,
        // construct TimePoint to return if necessary,
        // and update peek, rpeek, accumulatedTime, and lastTime.
        //
        // First check if replacement is next
        if (rpeek != null && (peek == null || rpeek.getKey().noLongerThan(accumulatedDuration))) {
          // We may need to create a TimePoint.Delta before the Commit
          Duration delta = rpeek.getKey().minus(lastTime);
          // If this delta happens to be the same as the Delta in this.points, use the existing Delta
          if (peek != null && peek instanceof TimePoint.Delta tpd && tpd.delta().isEqualTo(delta)) {
            peek = null;  // means we used it and need the next one
            lastTime = rpeek.getKey();
            return tpd;
          }
          // Construct and return a TimePoint.Delta if non-zero
          if (delta.isPositive()) {
            TimePoint tp = new TimePoint.Delta(delta);
            lastTime = rpeek.getKey();
            return tp;
          }
          // Sanity check - delta must be zero here
          if (!delta.isZero()) throw new AssertionError();

          // If this is the same time as the next Commit (or Delta) on this.points, replace and eat the TimePoint
          if (lastTime.isEqualTo(accumulatedDuration)) {
            peek = null;  // means we used it and need the next one
          }

          // Now, finally construct a Commit from the replacement EventGraph
          TimePoint tp = new TimePoint.Commit(rpeek.getValue(), topicsForEventGraph.get(rpeek.getValue()));
          rpeek = null; // means we used it and need the next one
          return tp;
        }
        // Check if the original TimePoint is next
        if (peek != null && (rpeek == null || rpeek.getKey().longerThan(accumulatedDuration))) {
          // If this TimePoint is a Delta, make sure we get the change in time (aka delta) since lastTime
          if (peek instanceof TimePoint.Delta d) {
            final TimePoint tp;
            // Reuse the existing Delta if we can
            if (lastTime.plus(d.delta()).isEqualTo(accumulatedDuration)) {
              tp = d;
            } else {
              tp = new TimePoint.Delta(accumulatedDuration.minus(lastTime));
            }
            lastTime = accumulatedDuration;
            peek = null;  // means we used it and need the next one
            return tp;
          }
          // peek is an unreplaced Commit; return it
          var commit = peek;
          peek = null;  // means we used it and need the next one
          return commit;
        }
        // Shouldn't get here
        throw new AssertionError("Impossible case in TemporalEventSourceDelta.next()");
      }
    };
  }

}
