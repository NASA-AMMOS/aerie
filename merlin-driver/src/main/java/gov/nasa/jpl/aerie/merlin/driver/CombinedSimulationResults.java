package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CombinedSimulationResults implements SimulationResultsInterface {

  final SimulationResultsInterface nr;
  final SimulationResultsInterface or;
  final TemporalEventSource timeline;


  public CombinedSimulationResults(SimulationResultsInterface newSimulationResults,
                                   SimulationResultsInterface oldSimulationResults,
                                   TemporalEventSource timeline) {
    this.nr = newSimulationResults;
    this.or = oldSimulationResults;
    this.timeline = timeline;
  }



  @Override
  public Instant getStartTime() {
    return ObjectUtils.min(nr.getStartTime(), or.getStartTime());
  }

  @Override
  public Duration getDuration() {
    return Duration.minus(ObjectUtils.max(Duration.addToInstant(nr.getStartTime(), nr.getDuration()),
                                          Duration.addToInstant(or.getStartTime(), or.getDuration())),
                          getStartTime());
  }

  @Override
  public Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> getRealProfiles() {
    return Stream.of(or.getRealProfiles(), nr.getRealProfiles()).flatMap(m -> m.entrySet().stream())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                           (pOld, pNew) -> mergeProfiles(or.getStartTime(), nr.getStartTime(),
                                                                         pOld, pNew, timeline)));
  }

  // We need to pass startTimes for both to know from where they are offset?  We don't want to assume that the two
  // simulations had the same timeframe.
  static <D> Pair<ValueSchema, List<ProfileSegment<D>>> mergeProfiles(Instant tOld, Instant tNew,
                                                                      Pair<ValueSchema, List<ProfileSegment<D>>> pOld,
                                                                      Pair<ValueSchema, List<ProfileSegment<D>>> pNew,
                                                                      TemporalEventSource timeline) {
    // We assume that the two ValueSchemas are the same and don't check for the sake of minimizing computation.
    return Pair.of(pOld.getLeft(), mergeSegmentLists(tOld, tNew, pOld.getRight(), pNew.getRight(), timeline));
  }

  static int ctr = 0;
  /**
   * Merge {@link ProfileSegment}s from an old simulation into those of a new one, replacing the old with the new.
   *
   * @param tOld start time of the old plan/simulation to correlate offsets
   * @param tNew start time of the new plan/simulation to correlate offsets
   * @param listOld old list of {@link ProfileSegment}s
   * @param listNew new list of {@link ProfileSegment}s
   * @param timeline the {@link TemporalEventSource} from the new simulation to determine where segments should be removed when the segment information isn't enough
   * @return the combined list of {@link ProfileSegment}s
   * @param <D>
   */
  private static <D> List<ProfileSegment<D>> mergeSegmentLists(Instant tOld, Instant tNew,
                                                               List<ProfileSegment<D>> listOld,
                                                               List<ProfileSegment<D>> listNew,
                                                               TemporalEventSource timeline) {
    int i = ctr++;
    // Find difference in simulation start times in the case that the simulations started at different times
    Duration offset = Duration.minus(tNew, tOld);
    // The time elapsed in each of the simulations
    final Duration[] elapsed = {Duration.ZERO, Duration.ZERO};  // need a final variable to satisfy lambda syntax but that allows us to reassign inside.
    // Initialize the times elapsed based on the difference in simulation start times
    if (offset.isNegative()) {
      elapsed[0] = elapsed[0].minus(offset);
    } else {
      elapsed[1] = elapsed[1].plus(offset);
    }

    var sOld = listOld.stream();
    var sNew = listNew.stream();

    // translate the segment extents into time elapsed.
    var ssOld = sOld.map(p -> {
      var r =  Triple.of(elapsed[0], 1, p);  // This middle index distinguishes old vs new and orders new before old when at the same time.
      elapsed[0] = elapsed[0].plus(p.extent());
      return r;
    });
    var ssNew = sNew.map(p -> {
      var r =  Triple.of(elapsed[1], 0, p);
      elapsed[1] = elapsed[1].plus(p.extent());
      final Triple<Duration, Integer, ProfileSegment<D>> r1 = r;
      return r1;
    });

    // Place a dummy triple at the end of the sorted triples since we need to look at two triples to handle ties in triples with the same time.
    final Triple<Duration, Integer, ProfileSegment<D>> tripleNull = Triple.of(null, null, null);
    final Stream<Triple<Duration, Integer, ProfileSegment<D>>> sorted =
        Stream.concat(Stream.of(ssOld, ssNew).flatMap(s -> s).sorted(), Stream.of(tripleNull));

    // Need a final to satisfy lambda syntax below, but we need to reassign so we enclose in an array.
    final Triple<Duration, Integer, ProfileSegment<D>>[] last = new Triple[] {null};
    var sss = sorted.map(t -> {
      final var lastTriple = last[0];
      last[0] = t; // for the next iteration
      Duration extent = null;

      // We need to look at two triples at a time, so we skip the first iteration.  Nulls will be stripped out later.
      if (lastTriple == null) {
        System.out.println("" + i + " skip first iteration");
        return null;
      }

      // This is the last pair of triples, the last being (null, null, null).  Just return the segment in the
      // last non-null triple, lastTriple.
      if (t == tripleNull) {
        System.out.println("" + i + " keeping last " + lastTriple);
        return lastTriple.getRight();
      }

        // Compute the duration between triples, translating elapsed time back into segment durations/extents
        extent = t.getLeft().minus(lastTriple.getLeft());

        // If the times are the same (extent == 0), and the new/vs old indices are different, then the new
        // segment replaces the old.  lastTriple is the new triple because of the middle index ordering.
        // We do the replacement by remembering the new (lastTriple) instead of the old (t) for the next
        // iteration and return nothing in this iteration, thus, skipping the old.
        if (extent.isEqualTo(Duration.ZERO) && !lastTriple.getMiddle().equals(t.getMiddle())) {
          System.out.println("" + i + " skipping " + t);
          last[0] = lastTriple;
          return null;
        }

      // We need to remove old segments where there are new events and no corresponding new segment.
      // We do this by remembering lastTriple instead of the old segment, t.
      if (timeline != null && t.getMiddle() == 1) {
        var commits = timeline.commitsByTime.get(lastTriple.getLeft());
        if (commits != null && commits.isEmpty()) {
          System.out.println("" + i + " skipping removed " + t);
          last[0] = lastTriple;
          return null;
        }
      }

      // Return a profile segment based on oldTriple and the time difference with t
      System.out.println("" + i + " keeping " + lastTriple);
      var p = new ProfileSegment<D>(extent, lastTriple.getRight().dynamics());
      return p;
    });

    // remove the nulls, representing skipped, replaced, removed, and non-existent segments
    var rsss = sss.filter(Objects::nonNull);

    // convert Stream to List
    return rsss.toList();
  }

  private static void testMergeSegmentLists() {
    ProfileSegment<Integer> p1 = new ProfileSegment<>(Duration.of(2, Duration.MINUTES), 0);
    ProfileSegment<Integer> p2 = new ProfileSegment<>(Duration.of(5, Duration.MINUTES), 1);
    ProfileSegment<Integer> p3 = new ProfileSegment<>(Duration.of(5, Duration.MINUTES), 2);

    ProfileSegment<Integer> p0 = new ProfileSegment<>(Duration.of(15, Duration.MINUTES), 0);
    Instant t = Instant.ofEpochSecond(366L * 24 * 3600 * 60);
    var list1 = List.of(p1, p2, p3);
    System.out.println(list1);
    var list2 = List.of(p0);
    System.out.println(list2);
    var list3 = mergeSegmentLists(t, t, list2, list1, null);
    System.out.println("merged list3");
    System.out.println(list3);
    list3 = mergeSegmentLists(t, t, list2, list1, null);
    System.out.println("merged list3");
    System.out.println(list3);
  }
  public static void main(final String[] args) {
    testMergeSegmentLists();
  }

  // TODO: Looking to modify interleave into a mergeSorted() to merge ProfileSegment Lists, but also need to combine elements.
  //       This wouldn't really avoid any of the messy stuff above, but there's a chance for an efficient Stream.
  public static <T extends Comparable<T>> Stream<T> interleave(Stream<? extends T> a, Stream<? extends T> b) {
    Spliterator<? extends T> spA = a.spliterator(), spB = b.spliterator();
    long s = spA.estimateSize() + spB.estimateSize();
    if(s < 0) s = Long.MAX_VALUE;  // s is negative if there's overflow from addition above
    int ch = spA.characteristics() & spB.characteristics()
             & (Spliterator.NONNULL|Spliterator.SIZED); //|Spliterator.SORTED  // if merging in order instead of interleaving
    ch |= Spliterator.ORDERED;

    return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(s, ch) {
      Spliterator<? extends T> sp1 = spA, sp2 = spB;

      @Override
      public boolean tryAdvance(final Consumer<? super T> action) {
        Spliterator<? extends T> sp = sp1;
        if(sp.tryAdvance(action)) {
          sp1 = sp2;
          sp2 = sp;
          return true;
        }
        return sp2.tryAdvance(action);
      }
    }, false);
  }

  @Override
  public Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> getDiscreteProfiles() {
    return Stream.of(or.getDiscreteProfiles(), nr.getDiscreteProfiles()).flatMap(m -> m.entrySet().stream())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                           (p1, p2) -> mergeProfiles(or.getStartTime(), nr.getStartTime(), p1, p2, timeline)));
  }

  @Override
  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    var combined = new HashMap<>(or.getSimulatedActivities());
    combined.putAll(nr.getSimulatedActivities());
    nr.getRemovedActivities().forEach(simActId -> combined.remove(simActId));
    return combined;
  }

  /**
   * @return
   */
  @Override
  public Set<SimulatedActivityId> getRemovedActivities() {
    var combined = new HashSet<>(or.getRemovedActivities());
    combined.addAll(nr.getRemovedActivities());
    return combined;
  }

  @Override
  public Map<SimulatedActivityId, UnfinishedActivity> getUnfinishedActivities() {
    var combined = new HashMap<>(or.getUnfinishedActivities());
    combined.putAll(nr.getUnfinishedActivities());
    return combined;
  }

  @Override
  public List<Triple<Integer, String, ValueSchema>> getTopics() {
    // WARNING: Assuming the same topics in old and new!!!
    return nr.getTopics();
  }

  @Override
  public Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> getEvents() {
    var ors = or.getEvents().entrySet().stream().map(e -> Pair.of(e.getKey().plus(Duration.minus(or.getStartTime(),getStartTime())), e.getValue()));
    var nrs = nr.getEvents().entrySet().stream().map(e -> Pair.of(e.getKey().plus(Duration.minus(nr.getStartTime(),getStartTime())), e.getValue()));
    // overwrite old with new where at the same time
    return Stream.of(ors, nrs).flatMap(s -> s)
                 .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (list1, list2) -> list2));
  }

  @Override
  public String toString() {
    return makeString();
  }
}
