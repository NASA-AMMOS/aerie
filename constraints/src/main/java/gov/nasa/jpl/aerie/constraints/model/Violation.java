package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Violation {
  public final List<Long> activityInstanceIds;
  public final List<String> resourceNames;
  public final List<Interval> violationWindows;

  public Violation(final List<Long> activityInstanceIds, final List<String> resourceNames, final Iterable<Interval> violationWindows) {
    this.activityInstanceIds = new ArrayList<>(activityInstanceIds);
    this.resourceNames = new ArrayList<>(resourceNames);
    this.violationWindows = new ArrayList<>();
    for (final var interval: violationWindows) this.violationWindows.add(interval);
  }

  public Violation(final Iterable<Interval> violationWindows) {
    this(new ArrayList<>(), new ArrayList<>(), violationWindows);
  }

  public Violation(final List<Long> activityInstanceIds, final List<String> resourceNames, final List<Interval> intervals) {
    this.activityInstanceIds = new ArrayList<>(activityInstanceIds);
    this.resourceNames = List.copyOf(resourceNames);
    this.violationWindows = List.copyOf(intervals);
  }

  public Violation(final Violation other) {
    this(other.activityInstanceIds, other.resourceNames, other.violationWindows);
  }

  public void addActivityId(final long activityId) {
    this.activityInstanceIds.add(0, activityId);
  }

  public String prettyPrint(final String prefix) {
    StringBuilder s = new StringBuilder(prefix).append("{\n");
    s.append(prefix).append("  Activity IDs: [");
    final var iter = this.activityInstanceIds.iterator();
    while (iter.hasNext()) {
      s.append(" ").append(iter.next());
      if (iter.hasNext()) s.append(",");
    }
    s.append(prefix).append("  Resource Names: [");
    final var resources = this.resourceNames.iterator();
    while (resources.hasNext()) {
      s.append(" ").append(resources.next());
      if (resources.hasNext()) s.append(",");
    }

    s.append(" ],\n").append(prefix).append("  Windows: [").append(violationWindows).append("]\n");
    s.append("}");
    return s.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Violation)) return false;
    final var other = (Violation)obj;
    return Objects.equals(this.activityInstanceIds, other.activityInstanceIds) &&
           Objects.equals(this.resourceNames, other.resourceNames) &&
           Objects.equals(this.violationWindows, other.violationWindows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityInstanceIds, this.resourceNames, this.violationWindows);
  }
}
