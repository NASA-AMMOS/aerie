package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public record Violation(List<Interval> violationIntervals, List<Long> activityInstanceIds) {
  public void addActivityId(final long activityId) {
    this.activityInstanceIds.add(0, activityId);
  }
}
