package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonObject;
import java.util.List;

public record SimulationDataset(boolean canceled, String simStartTime, String simEndTime, List<SimulatedActivity> activities) {
  public record SimulatedActivity(
      int spanId,
      Integer directiveId,
      Integer parentId,
      String duration,
      String startTime,
      String startOffset,
      String type
      ) {
    public static SimulatedActivity fromJSON(JsonObject json) {
      return new SimulatedActivity(
          json.getInt("id"),
          json.isNull("activity_directive") ? null : json.getJsonObject("activity_directive").getInt("id"),
          json.isNull("parent_id") ? null : json.getInt("parent_id"),
          json.isNull("duration") ? null : json.getString("duration"),
          json.getString("start_time"),
          json.getString("start_offset"),
          json.getString("type"));
    }
  }

  public static SimulationDataset fromJSON(JsonObject json) {
    final var simActivities =
        json.getJsonArray("simulated_activities").getValuesAs(SimulatedActivity::fromJSON);
    return new SimulationDataset(
        json.getBoolean("canceled"),
        json.getString("simulation_start_time"),
        json.getString("simulation_end_time"),
        simActivities);
  }

}
