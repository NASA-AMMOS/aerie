package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CombinedSimulationResults implements SimulationResultsInterface {

  protected SimulationResultsInterface nr = null;
  protected SimulationResultsInterface or = null;

  public CombinedSimulationResults(SimulationResultsInterface newSimulationResults,
                                   SimulationResultsInterface oldSimulationResults) {
    this.nr = newSimulationResults;
    this.or = oldSimulationResults;
  }



  @Override
  public Instant getStartTime() {
    return ObjectUtils.min(nr.getStartTime(), or.getStartTime());
  }

  @Override
  public Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> getRealProfiles() {
    return Stream.of(or.getRealProfiles(), nr.getRealProfiles()).flatMap(m -> m.entrySet().stream())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (p1, p2) -> mergeProfiles(or.getStartTime(), nr.getStartTime(), p1, p2)));
  }

  // We need to pass startTimes for both to know from where they are offset?  We don't want to assume that the two
  // simulations had the same timeframe.
  static <D> Pair<ValueSchema, List<ProfileSegment<D>>> mergeProfiles(Instant t1, Instant t2,
                                                                             Pair<ValueSchema, List<ProfileSegment<D>>> p1,
                                                                             Pair<ValueSchema, List<ProfileSegment<D>>> p2) {
    // We assume that the two ValueSchemas are the same and don't check for the sake of minimizing computation.
    return Pair.of(p1.getLeft(), mergeSegmentLists(t1, t2, p1.getRight(), p2.getRight()));
  }

  private static <D> List<ProfileSegment<D>> mergeSegmentLists(Instant t1, Instant t2,
                                                                      List<ProfileSegment<D>> list1,
                                                                      List<ProfileSegment<D>> list2) {
    Duration offset = Duration.minus(t2, t1);
    var s1 = list1.stream();
    var s2 = list2.stream();
    final Duration[] elapsed = {Duration.ZERO, Duration.ZERO};
    if (offset.isNegative()) {
      elapsed[0] = elapsed[0].minus(offset);
    } else {
      elapsed[1] = elapsed[1].plus(offset);
    }
    var ss1 = s1.map(p -> {
      var r =  Triple.of(elapsed[0], 1, p);
      elapsed[0] = elapsed[0].plus(p.extent());
      return r;
    });
    var ss2 = s2.map(p -> {
      var r =  Triple.of(elapsed[1], 0, p);
      elapsed[1] = elapsed[1].plus(p.extent());
      final Triple<Duration, Integer, ProfileSegment<D>> r1 = r;
      return r1;
    });
    var sorted = Stream.of(ss1, ss2).flatMap(s -> s).sorted();
    final Triple<Duration, Integer, ProfileSegment<D>>[] last;
    last = new Triple[] {null};
    var sss = sorted.map(t -> {
      Duration extent = last[0] == null ? t.getLeft() : t.getLeft().minus(last[0].getLeft());
      final var oldLast = last[0];
      if (extent.isEqualTo(Duration.ZERO) && oldLast != null && !oldLast.getMiddle().equals(t.getMiddle())) {
        return null;
      }
      last[0] = t;
      var p = new ProfileSegment<D>(extent, t.getRight().dynamics());
      return p;
    });
    var rsss = sss.filter(Objects::nonNull);

    return rsss.toList();
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
                                           (p1, p2) -> mergeProfiles(or.getStartTime(), nr.getStartTime(), p1, p2)));
    //return nr.getDiscreteProfiles();
  }

  @Override
  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    var combined = new HashMap<>(or.getSimulatedActivities());
    combined.putAll(nr.getSimulatedActivities());
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


  public static void main(String[] args) {
    System.out.println("Hello, World!");
    Long maxmax = Long.MAX_VALUE + Long.MAX_VALUE;
    System.out.println("" + maxmax);
    final int[] x = {0};
    var list1 = List.of(1,3,6,8).stream().map(i -> {
      var r = Pair.of(x[0], i);
      x[0] += i;
      return r;
    }).toList();
    System.out.println(list1);
    //collect($ -> 0, (a, b) -> Pair.of(a + b, b), (a, b) -> Pair.of(a + b, b));
    var list2 = List.of(2,3,5,9);
    //var list3 = Stream.of(list1, list2).flatMap(l -> l.stream()).
  }
}
