package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public final class Violation {
  public final List<Long> activityInstanceIds;
  public final List<String> resourceNames;
  public final List<Interval> violationWindows;
  public final List<Interval> gaps;

  public Violation(final List<Long> activityInstanceIds, final List<String> resourceNames, final Windows violationWindows) {
    this.activityInstanceIds = new ArrayList<>(activityInstanceIds);
    this.resourceNames = new ArrayList<>(resourceNames);
    this.violationWindows = new ArrayList<>(violationWindows.size());
    this.gaps = new ArrayList<>();
    for (final var interval: violationWindows.iterateEqualTo(true)) {
      this.violationWindows.add(interval);
    }

    // Set all known segments to false and assign true to the gaps
    final var gapsWindows = violationWindows.notEqualTo(violationWindows).assignGaps(new Windows(true));

    for (final var interval: gapsWindows.iterateEqualTo(true)) {
      this.gaps.add(interval);
    }
  }

  public Violation(final Windows violationWindows) {
    this(new ArrayList<>(), new ArrayList<>(), violationWindows);
  }

  public Violation(final List<Long> activityInstanceIds, final List<String> resourceNames, final List<Interval> intervals, final List<Interval> gaps) {
    this.activityInstanceIds = new ArrayList<>(activityInstanceIds);
    this.resourceNames = List.copyOf(resourceNames);
    this.violationWindows = List.copyOf(intervals);
    this.gaps = List.copyOf(gaps);
  }

  public Violation(final Violation other) {
    this(other.activityInstanceIds, other.resourceNames, other.violationWindows, other.gaps);
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
    s.append(prefix).append(" ], Resource Names: [");
    final var resources = this.resourceNames.iterator();
    while (resources.hasNext()) {
      s.append(" ").append(resources.next());
      if (resources.hasNext()) s.append(",");
    }

    s.append(" ],\n").append(prefix).append("  Windows: [").append(violationWindows).append("]\n");
    s.append(" ],\n").append(prefix).append("  Gaps: [").append(gaps).append("]\n");
    s.append("}");
    return s.toString();
  }

  @Override
  public String toString() {
    return prettyPrint("");
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof final Violation other)) return false;
    return Objects.equals(this.activityInstanceIds, other.activityInstanceIds) &&
           Objects.equals(this.resourceNames, other.resourceNames) &&
           Objects.equals(this.violationWindows, other.violationWindows) &&
           Objects.equals(this.gaps, other.gaps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityInstanceIds, this.resourceNames, this.violationWindows, this.gaps);
  }
}
