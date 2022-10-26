package gov.nasa.jpl.aerie.scheduler.server.http;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashMap;
import java.util.Map;
import gov.nasa.jpl.aerie.json.Breadcrumb;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.SchemaCache;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.services.MissionModelService;

public class ActivityTemplateJsonParser implements JsonParser<SchedulingDSL.ActivityTemplate> {

  private final Map<String, MissionModelService.ActivityType> activityTypesByName = new HashMap<>();

  public ActivityTemplateJsonParser(MissionModelService.MissionModelTypes activityTypes){
    activityTypes.activityTypes().forEach((actType)-> activityTypesByName.put(actType.name(), actType));
  }

  @Override
  public JsonObject getSchema(final SchemaCache anchors) {
    return Json
        .createObjectBuilder()
        .add("type", "object")
        .build();
  }

  @Override
  public JsonParseResult<SchedulingDSL.ActivityTemplate> parse(final JsonValue json) {
    if (!(json instanceof JsonObject)) return JsonParseResult.failure("expected object");
    final var asObject = json.asJsonObject();
    if(!asObject.containsKey("activityType") || !asObject.containsKey("args")) return JsonParseResult.failure("expected fields activityType and args");
    final var activityType = asObject.getString("activityType");
    final var args = asObject.getJsonObject("args");
    final var map = new HashMap<String, SerializedValue>(json.asJsonObject().size());
    for (final var field : args.entrySet()) {
      final var parsedValue = new StrictSerializedValueJsonParser(this.activityTypesByName.get(activityType).parameters().get(field.getKey())).parse(field.getValue());
      if (parsedValue instanceof JsonParseResult.Failure<SerializedValue> failure){
        failure.prependBreadcrumb(Breadcrumb.ofString(field.getKey()));
        return failure.cast();
      }
      map.put(field.getKey(), parsedValue.getSuccessOrThrow());
    }
    return JsonParseResult.success(new SchedulingDSL.ActivityTemplate(activityType,map));
  }


  @Override
  public JsonValue unparse(final SchedulingDSL.ActivityTemplate activityTemplate) {
    final var builder = Json.createObjectBuilder();
    builder.add("activityType", activityTemplate.activityType());
    final var argumentsBuilder = Json.createObjectBuilder();
    for (final var entry : activityTemplate.arguments().entrySet()) {
      argumentsBuilder.add(
          entry.getKey(),
          new StrictSerializedValueJsonParser(
              this.activityTypesByName
                  .get(activityTemplate.activityType())
                  .parameters()
                  .get(entry.getKey()))
              .unparse(entry.getValue()));
    }
    builder.add("args", argumentsBuilder.build());
    return builder.build();
  }
}
