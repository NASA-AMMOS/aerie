package gov.nasa.jpl.aerie.e2e.types;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;


public record ActivityType(String name, Map<String, Parameter> parameters) {
  public static ActivityType fromJSON(JsonObject json) {
    final var parameters = json.getJsonObject("parameters");
    final var parameterMap = new HashMap<String, Parameter>();
    for (final var parameterName : parameters.keySet()) {
      parameterMap.put(parameterName, Parameter.fromJSON(parameters.getJsonObject(parameterName)));
    }
    return new ActivityType(json.getString("name"), parameterMap);
  }

  public record Parameter(int order, ValueSchema schema) {
    public static Parameter fromJSON(JsonObject json) {
      return new Parameter(json.getInt("order"), ValueSchema.fromJSON(json.getJsonObject("schema")));
    }
  }
}
