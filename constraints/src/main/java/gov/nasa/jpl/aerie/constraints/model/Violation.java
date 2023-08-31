package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.ArrayList;
import java.util.List;

public record Violation(List<Interval> windows, ArrayList<Long> activityInstanceIds) {
  public Violation(List<Interval> windows, List<Long> activityInstanceIds) {
    this(windows, new ArrayList<>(activityInstanceIds));
  }
  public void addActivityId(final long activityId) {
    this.activityInstanceIds.add(0, activityId);
  }
}
