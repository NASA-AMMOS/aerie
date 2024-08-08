package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.Profile;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfilingState;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.Subscriptions;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskFrame;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Cell;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SubInstantDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceTracker {
  public static final boolean debug = false;
  private final Map<String, Resource<?>> resources = new HashMap<>();
  private final Map<String, ProfilingState<?>> resourceProfiles = new HashMap<>();

  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, String> waitingResources = new Subscriptions<>();

  private final Map<String, Duration> resourceExpiries = new HashMap<>();

  private final SimulationEngine engine;
  private final ResourceTrackerEventSource timeline;
  private LiveCells cells;
  private Duration elapsedTime;

  public ResourceTracker(final SimulationEngine engine, final LiveCells initialCells) {
    this.engine = engine;
    this.timeline = new ResourceTrackerEventSource(engine.timeline);
    this.cells = new LiveCells(this.timeline, initialCells);
    this.elapsedTime = Duration.ZERO;
  }


  public void track(final String name, final Resource<?> resource) {
    this.resourceProfiles.put(name, new ProfilingState<>(resource, new Profile<>()));
    this.resources.put(name, resource);
    this.resourceExpiries.put(name, this.elapsedTime);
  }

  public boolean isEmpty() {
    return !this.timeline.hasNext();
  }
  public boolean isEmpty(Duration endTime, boolean includeEndTime) {
    if (!this.timeline.hasNext()) return true;
    if (elapsedTime.longerThan(endTime)) return true;
    if (!includeEndTime && elapsedTime.isEqualTo(endTime)) return true;
    if (includeEndTime && elapsedTime.isEqualTo(endTime) && timepointPastEnd != null) return true;
    return false;
  }

  /**
   *  Because we can't simulate past a certain time point, and we use iterators that don't let us peek ahead,
   *  we need to remember the last TimePoint when we've stepped too far and process it later when we move
   *  ahead more.
   */
  private TemporalEventSource.TimePoint timepointPastEnd = null;

  /**
   * Post condition: timeline will be stepped up to the endpoint
   */
  public void updateResources(Duration endTime, boolean includeEndTime) {
    if (this.isEmpty(endTime, includeEndTime)) return;

    TemporalEventSource.TimePoint timePoint = timepointPastEnd;
    timepointPastEnd = null;
    if (timePoint == null) {
      timePoint = this.timeline.next();
    }
    if (debug) System.out.println("updateResources(): " + elapsedTime + " -- timeline.next() -> " + timePoint);
    if (timePoint instanceof TemporalEventSource.TimePoint.Delta p) {
      var timeAfterDelta = elapsedTime.plus(p.delta());
      // If this delta overshoots the endTime, split it into a delta up to the endTime, and one after
      // the end time to save for later.
      if (timeAfterDelta.longerThan(endTime) ||
          (!includeEndTime && timeAfterDelta.isEqualTo(endTime))) {
        var overshot = timeAfterDelta.minus(endTime);
        if (!overshot.isZero()) {
          timepointPastEnd = new TemporalEventSource.TimePoint.Delta(overshot);
          p = new TemporalEventSource.TimePoint.Delta(endTime);
        }
      }
      updateExpiredResources(p.delta()); // this call updates ourOwnTimeline and elapsedTime
    } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit p) {
      var topics = p.topics();
      if (timeline.timeline.oldTemporalEventSource != null) {
        topics = topics.stream().filter(
            t -> timeline.timeline.isTopicStale(t, new SubInstantDuration(elapsedTime, 0))).collect(Collectors.toSet());
      }
      expireInvalidatedResources(topics);
    } else {
      throw new Error("Unhandled variant of "
                      + TemporalEventSource.TimePoint.class.getCanonicalName()
                      + ": "
                      + timePoint);
    }
  }

  // waitingResources are those that after evaluation are waiting on a topic/cell to change, at which point they are
  // calculated and stored in the Profile.  When invalidating a topic, we have declared that the topic/cell is
  // (well might) change, and the resource should be updated.  When the resource is updated, it and its referenced topics
  // are added to waitingResources.  So, a resource is always in waitingResources but its referenced topics are replaced.
  // That's not obvious because the replacement is broken up into removing in one function and then adding back in another.
  public void invalidateTopic(final Topic<?> topic, final Duration invalidationTime) {
    if (invalidationTime.noLongerThan(invalidationTime)) {
      var resources = this.waitingResources.invalidateTopic(topic);
      if (debug) System.out.println("RT invalidate topic: " + topic + " and schedule expiries at " + invalidationTime + " for resources " + resources);
      for (final var resourceName : resources) {
        this.resourceExpiries.put(resourceName, this.elapsedTime);
        if (debug) System.out.println("RT resourceExpiries.put(resourceName=" + resourceName+", elapsedTime=" + invalidationTime + ")");
      }
    } else {
      // need to do this in the future
    }
  }

  private void expireInvalidatedResources(final Set<Topic<?>> invalidatedTopics) {
    for (final var topic : invalidatedTopics) {
      var resources = this.waitingResources.invalidateTopic(topic);
      if (debug) System.out.println("RT invalidate topic: " + topic + " and schedule expiries at " + this.elapsedTime + " for resources " + resources);
      for (final var resourceName : resources) {
        this.resourceExpiries.put(resourceName, this.elapsedTime);
        if (debug) System.out.println("RT resourceExpiries.put(resourceName=" + resourceName+", elapsedTime=" + elapsedTime + ")");
      }
    }
  }

  private void updateExpiredResources(final Duration delta) {
    final var endTime = this.elapsedTime.plus(delta);

    while (!this.resourceExpiries.isEmpty()) {
      final var nextExpiry = this.resourceExpiries
          .entrySet()
          .stream()
          .min(Map.Entry.comparingByValue())
          .orElseThrow();

      final var resourceName = nextExpiry.getKey();
      final var resourceQueryTime = nextExpiry.getValue();

      if (resourceQueryTime.longerThan(endTime)) break;

      if (!resourceQueryTime.isEqualTo(this.elapsedTime)) {
        this.timeline.advance(resourceQueryTime.minus(this.elapsedTime));
        this.elapsedTime = resourceQueryTime;
      }

      this.resourceExpiries.remove(resourceName);
      // Compute the new resource value and add to the Profile
      TaskFrame.run(this.resources.get(resourceName), this.cells, (job, frame) -> {
        final var querier = engine.new EngineQuerier(new SubInstantDuration(this.elapsedTime, 0), frame);
        this.resourceProfiles.get(resourceName).append(resourceQueryTime, querier);
        if (debug) System.out.println("RT profile updated for " + resourceName + ": " + resourceProfiles.get(resourceName));
        this.waitingResources.subscribeQuery(resourceName, querier.referencedTopics);
        if (debug) System.out.println("RT querier, " + querier + " subscribing " + resourceName + " to referenced topics: " + querier.referencedTopics);

        final Optional<Duration> expiry = querier.expiry.map(d -> resourceQueryTime.plus((Duration)d));
        // This resource's no-later-than query time needs to be updated
        expiry.ifPresent(duration -> {
          this.resourceExpiries.put(resourceName, duration);
          if (debug) System.out.println("RT resourceExpiries.put(resourceName=" + resourceName+", duration=" + duration + ") at " + elapsedTime);
        });
      });
    }

    this.elapsedTime = endTime;
  }

  public Map<String, ProfilingState<?>> resourceProfiles() {
    return this.resourceProfiles;
  }

  public void reset() {
    this.cells = new LiveCells(this.timeline, engine.getMissionModel().getInitialCells());
    this.elapsedTime = Duration.ZERO;
    (new HashSet<String>(this.resources.keySet())).forEach(name -> track(name, resources.get(name)));
    this.timepointPastEnd = null;
  }

  /**
   * @param pointCount Index into input timeline
   * @param timeAfterPoint Offset from the point indicated by pointCount
   */
  private record DenseTime(int pointCount, Duration timeAfterPoint) {}

  static class ResourceTrackerEventSource implements EventSource, Iterator<TemporalEventSource.TimePoint> {

    private final TemporalEventSource timeline;
    private final Iterator<TemporalEventSource.TimePoint> timelineIterator;
    private DenseTime limit;
    private boolean brad = true;

    public ResourceTrackerEventSource(final TemporalEventSource timeline) {
      this.timeline = timeline;
      this.timelineIterator = timeline.iterator();
      this.limit = new DenseTime(-1, Duration.ZERO); // The caller gets the next point with next(), and the cells can see all but that last point
    }

    void advance(final Duration delta) {
      if (delta.isNegative()) throw new RuntimeException("Cannot advance back in time");
      this.limit = new DenseTime(this.limit.pointCount(), this.limit.timeAfterPoint().plus(delta));
    }

    @Override
    public Cursor cursor() {
      return new Cursor() {
        private final Iterator<TemporalEventSource.TimePoint> timelineIterator = ResourceTrackerEventSource.this.timeline.iterator();

        /* The history of an offset includes all points up to but not including timeline.get(pointCount) */
        private DenseTime offset = new DenseTime(0, Duration.ZERO);

        @Override
        public <State> Cell<State> stepUp(final Cell<State> cell) {
          System.out.println("stepUp(): BEGIN");
          if (brad) {
            timeline.stepUp(cell, SubInstantDuration.MAX_VALUE);
            return cell;
          }
          // Extend timeline iterator to the current limit
          for (var i = this.offset.pointCount; i < ResourceTrackerEventSource.this.limit.pointCount(); i++) {
            final var point = this.timelineIterator.next();
            if (debug) System.out.println("stepUp(): timelineIterator.next() -> " + point);

            if (point instanceof TemporalEventSource.TimePoint.Delta p) {
              cell.step(p.delta().minus(this.offset.timeAfterPoint()));
              this.offset = new DenseTime(i + 1, Duration.ZERO);
            } else if (point instanceof TemporalEventSource.TimePoint.Commit p) {
              if (!this.offset.timeAfterPoint().isZero()) {
                throw new AssertionError("Cannot have a non-zero offset from a Commit");
              }
              if (cell.isInterestedIn(p.topics())) cell.apply(timeline.withoutReadEvents(p.events()), null, false);
            } else {
              throw new IllegalStateException();
            }
          }

          final var remainingOffset = ResourceTrackerEventSource.this.limit.timeAfterPoint().minus(this.offset.timeAfterPoint());
          if (!remainingOffset.isZero()) {
            cell.step(remainingOffset);
          }

          this.offset = ResourceTrackerEventSource.this.limit;
          return cell;
        }
      };
    }

    @Override
    public boolean hasNext() {
      return this.timelineIterator.hasNext();
    }

    @Override
    public TemporalEventSource.TimePoint next() {
      this.limit = new DenseTime(this.limit.pointCount() + 1, Duration.ZERO);
      return this.timelineIterator.next();
    }
  }
}
