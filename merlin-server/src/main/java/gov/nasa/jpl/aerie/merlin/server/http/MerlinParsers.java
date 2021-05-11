package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonString;
import javax.json.JsonValue.ValueType;
import java.time.format.DateTimeParseException;
import java.util.Collections;
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
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry4;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry5;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  public static final JsonParser<Timestamp> timestampP =
      JsonParser.create(
          (json) -> {
            if (!(json instanceof JsonString)) return JsonParseResult.failure("expected string");
            try {
              return JsonParseResult.success(Timestamp.fromString(((JsonString) json).getString()));
            } catch (DateTimeParseException e) {
              return JsonParseResult.failure("invalid timestamp format");
            }
          },
          Json
              .createObjectBuilder()
              .add("type", "string")
              .add("format", "date-time")
              .build());

  public static final JsonParser<Duration> durationP
      = longP
      . map(microseconds -> Duration.of(microseconds, Duration.MICROSECONDS));

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

  public static final JsonParser<Pair<Duration, SerializedActivity>> scheduledActivityP
      = productP
      . field("defer", durationP)
      . field("type", stringP)
      . optionalField("parameters", mapP(serializedParameterP))
      . map(uncurry3(defer -> type -> parameters ->
          Pair.of(defer, new SerializedActivity(type, parameters.orElse(Collections.emptyMap())))));

  public static final JsonParser<CreateSimulationMessage> createSimulationMessageP
      = productP
      . field("adaptationId", stringP)
      . field("startTime", timestampP.map(Timestamp::toInstant))
      . field("samplingDuration", durationP)
      . field("activities", mapP(scheduledActivityP))
      . map(uncurry4(adaptationId -> startTime -> samplingDuration -> activities ->
          new CreateSimulationMessage(adaptationId, startTime, samplingDuration, activities)));

  public static final JsonParser<Constraint> constraintP
      = productP
      . field("name", stringP)
      . field("summary", stringP)
      . field("description", stringP)
      . field("definition", stringP)
      .map(uncurry4(name -> summary -> description -> definition ->
          new Constraint(name, summary, description, definition)));
}
