package gov.nasa.jpl.aerie.scheduler.model;

import java.util.List;

public interface ActivityTypeList {
  record Whitelist(List<ActivityType> activityExpressions) implements ActivityTypeList {}

  record Blacklist(List<ActivityType> activityExpressions) implements ActivityTypeList {}

  static ActivityTypeList whitelist(final List<ActivityType> activityExpressions) {
    return new Whitelist(activityExpressions);
  }

  static ActivityTypeList blacklist(final List<ActivityType> activityExpressions) {
    return new Blacklist(activityExpressions);
  }

  static ActivityTypeList matchAny() {
    return blacklist(List.of());
  }

  static ActivityTypeList empty() {
    return whitelist(List.of());
  }
}
