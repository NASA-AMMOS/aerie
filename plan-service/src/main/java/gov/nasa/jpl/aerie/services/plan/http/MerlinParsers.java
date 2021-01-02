package gov.nasa.jpl.aerie.services.plan.http;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.services.plan.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.plan.models.NewPlan;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.Timestamp;

import javax.json.JsonString;
import javax.json.JsonValue.ValueType;
import java.time.format.DateTimeParseException;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.nullP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry3;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry5;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  public static final JsonParser<Timestamp> timestampP = json -> {
    if (!(json instanceof JsonString)) return JsonParseResult.failure("expected string");
    try {
      return JsonParseResult.success(Timestamp.fromString(((JsonString)json).getString()));
    } catch (DateTimeParseException e) {
      return JsonParseResult.failure("invalid timestamp format");
    }
  };

  public static final JsonParser<SerializedValue> serializedParameterP =
      recursiveP(selfP -> BasicParsers
          . <SerializedValue>sumP()
          . when(ValueType.NULL,
                 nullP.map(SerializedValue::of))
          . when(ValueType.TRUE,
                 boolP.map(SerializedValue::of))
          . when(ValueType.FALSE,
                 boolP.map(SerializedValue::of))
          . when(ValueType.STRING,
                 stringP.map(SerializedValue::of))
          . when(ValueType.NUMBER, chooseP(
              longP.map(SerializedValue::of),
              doubleP.map(SerializedValue::of)))
          . when(ValueType.ARRAY,
                 listP(selfP).map(SerializedValue::of))
          . when(ValueType.OBJECT,
                 mapP(selfP).map(SerializedValue::of)));

  public static final JsonParser<ActivityInstance> activityInstanceP
      = productP
      . field("type", stringP)
      . field("startTimestamp", timestampP)
      . field("parameters", mapP(serializedParameterP))
      .map(uncurry3(type -> startTimestamp -> parameters ->
          new ActivityInstance(type, startTimestamp, parameters)));

  public static final JsonParser<ActivityInstance> activityInstancePatchP
      = productP
      . optionalField("type", stringP)
      . optionalField("startTimestamp", timestampP)
      . optionalField("parameters", mapP(serializedParameterP))
      .map(uncurry3(type -> startTimestamp -> parameters ->
          new ActivityInstance(type.orElse(null), startTimestamp.orElse(null), parameters.orElse(null))));

  public static final JsonParser<NewPlan> newPlanP
      = productP
      . field("name", stringP)
      . field("adaptationId", stringP)
      . field("startTimestamp", timestampP)
      . field("endTimestamp", timestampP)
      . optionalField("activityInstances", listP(activityInstanceP))
      .map(uncurry5(name -> adaptationId -> startTimestamp -> endTimestamp -> activityInstances ->
          new NewPlan(name, adaptationId, startTimestamp, endTimestamp, activityInstances.orElse(List.of()))));

  public static final JsonParser<Plan> planPatchP
      = productP
      . optionalField("name", stringP)
      . optionalField("adaptationId", stringP)
      . optionalField("startTimestamp", timestampP)
      . optionalField("endTimestamp", timestampP)
      . optionalField("activityInstances", mapP(activityInstanceP))
      . map(uncurry5(name -> adaptationId -> startTimestamp -> endTimestamp -> activityInstances ->
          new Plan(
              name.orElse(null),
              adaptationId.orElse(null),
              startTimestamp.orElse(null),
              endTimestamp.orElse(null),
              activityInstances.orElse(null))));
}
