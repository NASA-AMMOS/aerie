package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface StartTime {
  record OffsetFromPlanStart(Duration offset) implements StartTime {}
  record OffsetFromActivityStart(ActivityInstanceId activityId, Duration offset) implements StartTime {}
  record OffsetFromActivityEnd(ActivityInstanceId activityId, Duration offset) implements StartTime {}
}
