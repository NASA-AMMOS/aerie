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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ResourceTracker {
  private final Map<String, Resource<?>> resources = new HashMap<>();
  private final Map<String, ProfilingState<?>> resourceProfiles = new HashMap<>();

  /** The set of queries depending on a given set of topics. */
  private final Subscriptions<Topic<?>, String> waitingResources = new Subscriptions<>();

  private final Map<String, Duration> resourceExpiries = new HashMap<>();

  private final SimulationEngine engine;
  private final ResourceTrackerEventSource timeline;
  private final LiveCells cells;
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

  /**
   * Post condition: timeline will be stepped up to the endpoint
   */
  public void updateResources() {
    if (this.isEmpty()) return;
    final var timePoint = this.timeline.next();
    if (timePoint instanceof TemporalEventSource.TimePoint.Delta p) {
      updateExpiredResources(p.delta()); // this call updates ourOwnTimeline and elapsedTime
    } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit p) {
      expireInvalidatedResources(p.topics());
    } else {
      throw new Error("Unhandled variant of "
                      + TemporalEventSource.TimePoint.class.getCanonicalName()
                      + ": "
                      + timePoint);
    }
  }

  private void expireInvalidatedResources(final Set<Topic<?>> invalidatedTopics) {
    for (final var topic : invalidatedTopics) {
      var resources = this.waitingResources.invalidateTopic(topic);
      //System.out.println("RT invalidate topic: " + topic + " and schedule expiries at " + this.elapsedTime + " for resources " + resources);
      for (final var resourceName : resources) {
        this.resourceExpiries.put(resourceName, this.elapsedTime);
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

      this.timeline.advance(resourceQueryTime.minus(this.elapsedTime));
      this.elapsedTime = this.elapsedTime.plus(resourceQueryTime.minus(this.elapsedTime));

      this.resourceExpiries.remove(resourceName);
      TaskFrame.run(this.resources.get(resourceName), this.cells, (job, frame) -> {
        final var querier = engine.new EngineQuerier(this.elapsedTime, frame);
        this.resourceProfiles.get(resourceName).append(resourceQueryTime, querier);
        this.waitingResources.subscribeQuery(resourceName, querier.referencedTopics);
//        System.out.println("RT querier, " + querier + " subscribing " + resourceName + " to referenced topics: " + querier.referencedTopics);

        final Optional<Duration> expiry = querier.expiry.map(d -> resourceQueryTime.plus((Duration)d));
        // This resource's no-later-than query time needs to be updated
        expiry.ifPresent(duration -> this.resourceExpiries.put(resourceName, duration));
      });
    }

    this.elapsedTime = endTime;
  }

  public Map<String, ProfilingState<?>> resourceProfiles() {
    return this.resourceProfiles;
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
        public void stepUp(final Cell<?> cell) {
          if (brad) {
            timeline.stepUp(cell, Duration.MAX_VALUE, true);
            return;
          }
          // Extend timeline iterator to the current limit
          for (var i = this.offset.pointCount; i < ResourceTrackerEventSource.this.limit.pointCount(); i++) {
            final var point = this.timelineIterator.next();

            if (point instanceof TemporalEventSource.TimePoint.Delta p) {
              cell.step(p.delta().minus(this.offset.timeAfterPoint()));
              this.offset = new DenseTime(i + 1, Duration.ZERO);
            } else if (point instanceof TemporalEventSource.TimePoint.Commit p) {
              if (!this.offset.timeAfterPoint().isZero()) {
                throw new AssertionError("Cannot have a non-zero offset from a Commit");
              }
              if (cell.isInterestedIn(p.topics())) cell.apply(p.events(), null, false);
            } else {
              throw new IllegalStateException();
            }
          }

          final var remainingOffset = ResourceTrackerEventSource.this.limit.timeAfterPoint().minus(this.offset.timeAfterPoint());
          if (!remainingOffset.isZero()) {
            cell.step(remainingOffset);
          }

          this.offset = ResourceTrackerEventSource.this.limit;
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
