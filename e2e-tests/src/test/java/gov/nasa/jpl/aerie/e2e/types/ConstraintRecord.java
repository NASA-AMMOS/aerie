package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.List;

public record ConstraintRecord(
      int constraintId,
      String constraintName,
      String type,
      List<String> resourceIds,
      List<ConstraintViolation> violations,
      List<Interval> gaps
  ) {
  public record ConstraintViolation(List<Integer> activityInstanceIds, List<Interval> windows) {
    public static ConstraintViolation fromJSON(JsonObject json) {
      return new ConstraintViolation(
          json.getJsonArray("activityInstanceIds")
              .getValuesAs(JsonNumber::intValue),
          json.getJsonArray("windows")
              .getValuesAs(Interval::fromJSON));
    }
  }

  public record Interval(long start, long end) {
    public static Interval fromJSON(JsonObject json) {
      return new Interval(json.getJsonNumber("start").longValue(), json.getJsonNumber("end").longValue());
    }
  }

  public static ConstraintRecord fromJSON(JsonObject json) {
    final var resourceIds = json.getJsonArray("resourceIds").getValuesAs(JsonString::getString);
    final var gaps = json.getJsonArray("gaps").getValuesAs(Interval::fromJSON);
    final var violations = json.getJsonArray("violations").getValuesAs(ConstraintViolation::fromJSON);

    return new ConstraintRecord(
        json.getInt("constraintId"),
        json.getString("constraintName"),
        json.getString("type"),
        resourceIds,
        violations,
        gaps
    );
  }
}
