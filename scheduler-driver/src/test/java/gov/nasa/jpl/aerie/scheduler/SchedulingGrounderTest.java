package gov.nasa.jpl.aerie.scheduler;

import com.google.common.testing.NullPointerTester;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulePlanGrounder;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulingGrounderTest {
  private final static PlanningHorizon h = new PlanningHorizon(TimeUtility.fromDOY("2025-001T01:01:01.001"), TimeUtility.fromDOY("2025-005T01:01:01.001"));
  private final static Duration t0 = h.getStartAerie();
  private final static Duration d1min = Duration.of(1, Duration.MINUTE);
  private final static Duration d1hr = Duration.of(1, Duration.HOUR);
  private final static Duration t1hr = t0.plus(d1hr);
  private final static Duration t2hr = t0.plus(d1hr.times(2));

  @Test
  public void test(){
    final var at = new ActivityType("at");
    final var id1 = new SchedulingActivityDirectiveId(1);
    final var id2 = new SchedulingActivityDirectiveId(2);
    final var id3 = new SchedulingActivityDirectiveId(3);
    final var act1 = SchedulingActivityDirective.of(id1, at, t0, d1min, null, true);
    final var act2 = SchedulingActivityDirective.of(id2, at, t1hr, d1min, act1.id(), false);
    final var act3 = SchedulingActivityDirective.of(id3, at, t2hr, d1min, act2.id(), false);
    final var acts = List.of(act1, act3, act2);
    final var result = SchedulePlanGrounder.groundSchedule(acts, h.getEndAerie());
    //act 2 should be [t1hr+d1min, t1hr + d1min + d1min]
    //act 3 should be [t1hr + d1min + d1min, t1hr + d1min + d1min + d1min]
    final var act1expt = new ActivityInstance(id1.id(), at.getName(), Map.of(), Interval.between(t0, t0.plus(d1min)));
    final var act2expt = new ActivityInstance(id2.id(), at.getName(), Map.of(), Interval.between(t1hr.plus(t0).plus(d1min), t1hr.plus(t0).plus(d1min).plus(d1min)));
    final var act3expt = new ActivityInstance(id3.id(), at.getName(), Map.of(), Interval.between(t2hr.plus(t1hr).plus(t0).plus(d1min).plus(d1min), t2hr.plus(t1hr).plus(t0).plus(d1min).plus(d1min).plus(d1min)));
    final var asSet = new HashSet<>(result.get());
    assertTrue(asSet.contains(act1expt));
    assertTrue(asSet.contains(act2expt));
    assertTrue(asSet.contains(act3expt));
  }

  @Test
  public void testEmptyDueToEmptyDuration(){
    final var at = new ActivityType("at");
    final var id1 = new SchedulingActivityDirectiveId(1);
    final var id2 = new SchedulingActivityDirectiveId(2);
    final var id3 = new SchedulingActivityDirectiveId(3);
    final var act1 = SchedulingActivityDirective.of(id1, at, t0, d1min, null, true);
    final var act2 = SchedulingActivityDirective.of(id2, at, t1hr, null, act1.id(), false);
    final var act3 = SchedulingActivityDirective.of(id3, at, t2hr, d1min, act2.id(), false);
    final var acts = List.of(act1, act3, act2);
    final var result = SchedulePlanGrounder.groundSchedule(acts, h.getEndAerie());
    assertEquals(Optional.empty(), result);
  }
}
