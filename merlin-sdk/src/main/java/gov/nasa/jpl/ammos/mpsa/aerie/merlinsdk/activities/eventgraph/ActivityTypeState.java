package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.UtilityMethods.collapseOverlapping;

public final class ActivityTypeState {
  private final String activityType;
  private final ActivityModelQuerier querier;

  public ActivityTypeState(final String activityType, final ActivityModelQuerier querier) {
    this.activityType = activityType;
    this.querier = querier;
  }

  public Constraint whenActive() {
    return this.exists(a -> a.whenActive());
  }

  public Constraint exists(final Function<ActivityInstanceState, Constraint> predicate) {
    return new Constraint() {
      @Override
      public Set<String> getActivityIds() {
        final var matchingIds = new HashSet<String>();

        for (var activityId : querier.getActivitiesOfType(activityType)) {
          final var constraint = predicate.apply(new ActivityInstanceState(activityId, querier));
          final var matchWindows = constraint.getWindows();
          if (matchWindows.isEmpty()) continue;

          matchingIds.addAll(constraint.getActivityIds());
        }

        return matchingIds;
      }

      @Override
      public Set<String> getStateIds() {
        return new HashSet<>();
      }

      @Override
      public List<Window> getWindows() {
        final var windows = new ArrayList<Window>();

        for (var activityId : querier.getActivitiesOfType(activityType)) {
          final var constraint = predicate.apply(new ActivityInstanceState(activityId, querier));
          final var matchWindows = constraint.getWindows();
          if (matchWindows.isEmpty()) continue;

          windows.addAll(matchWindows);
        }

        return collapseOverlapping(windows);
      }
    };
  }
}
