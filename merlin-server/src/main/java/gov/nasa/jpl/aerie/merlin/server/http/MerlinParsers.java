package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParseResult;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.json.Uncurry;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.services.CreateSimulationMessage;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry3;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry4;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry5;
import static gov.nasa.jpl.aerie.json.Uncurry.uncurry6;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;

public abstract class MerlinParsers {
  private MerlinParsers() {}

  public static final JsonParser<Timestamp> timestampP = new JsonParser<>() {
    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder(stringP.getSchema())
          .add("format", "date-time")
          .build();
    }

    @Override
    public JsonParseResult<Timestamp> parse(final JsonValue json) {
      final var result = stringP.parse(json);
      if (result instanceof JsonParseResult.Success<String> s) {
        try {
          return JsonParseResult.success(Timestamp.fromString(s.result()));
        } catch (DateTimeParseException e) {
          return JsonParseResult.failure("invalid timestamp format");
        }
      } else if (result instanceof JsonParseResult.Failure<?> f) {
        return f.cast();
      } else {
        throw new UnexpectedSubtypeError(JsonParseResult.class, result);
      }
    }

    @Override
    public JsonValue unparse(final Timestamp value) {
      return stringP.unparse(value.toString());
    }
  };

  public static final JsonParser<Duration> durationP
      = longP
      . map(Iso.of(
          microseconds -> Duration.of(microseconds, Duration.MICROSECONDS),
          duration -> duration.in(Duration.MICROSECONDS)));

  public static final JsonParser<ActivityInstance> activityInstanceP
      = productP
      . field("type", stringP)
      . field("startTimestamp", timestampP)
      . field("parameters", mapP(serializedValueP))
      . map(Iso.of(
          uncurry3(type -> startTimestamp -> parameters ->
              new ActivityInstance(type, startTimestamp, parameters)),
          activity -> Uncurry.tuple3(activity.type, activity.startTimestamp, activity.parameters)));

  public static final JsonParser<ActivityInstance> activityInstancePatchP
      = productP
      . optionalField("type", stringP)
      . optionalField("startTimestamp", timestampP)
      . optionalField("parameters", mapP(serializedValueP))
      . map(Iso.of(
          uncurry3(type -> startTimestamp -> parameters ->
              new ActivityInstance(type.orElse(null), startTimestamp.orElse(null), parameters.orElse(null))),
          $ -> Uncurry.tuple3(Optional.ofNullable($.type), Optional.ofNullable($.startTimestamp), Optional.ofNullable($.parameters))));

  public static final JsonParser<NewPlan> newPlanP
      = productP
      . field("name", stringP)
      . field("adaptationId", stringP)
      . field("startTimestamp", timestampP)
      . field("endTimestamp", timestampP)
      . optionalField("activityInstances", listP(activityInstanceP))
      . optionalField("configuration", mapP(serializedValueP))
      . map(Iso.of(
          uncurry6(name -> adaptationId -> startTimestamp -> endTimestamp -> activityInstances -> configuration ->
              new NewPlan(name, adaptationId, startTimestamp, endTimestamp, activityInstances.orElse(List.of()), configuration.orElse(Map.of()))),
          $ -> Uncurry.tuple6($.name, $.adaptationId, $.startTimestamp, $.endTimestamp, Optional.of($.activityInstances), Optional.of($.configuration))));

  public static final JsonParser<Plan> planPatchP
      = productP
      . optionalField("name", stringP)
      . optionalField("adaptationId", stringP)
      . optionalField("startTimestamp", timestampP)
      . optionalField("endTimestamp", timestampP)
      . optionalField("activityInstances", mapP(activityInstanceP))
      . optionalField("configuration", mapP(serializedValueP))
      . map(Iso.of(
          uncurry6(name -> adaptationId -> startTimestamp -> endTimestamp -> activityInstances -> configuration ->
              new Plan(
                  name.orElse(null),
                  adaptationId.orElse(null),
                  startTimestamp.orElse(null),
                  endTimestamp.orElse(null),
                  activityInstances.orElse(null),
                  configuration.orElse(Map.of()))),
          $ -> Uncurry.tuple6(
              Optional.ofNullable($.name),
              Optional.ofNullable($.adaptationId),
              Optional.ofNullable($.startTimestamp),
              Optional.ofNullable($.endTimestamp),
              Optional.ofNullable($.activityInstances),
              Optional.ofNullable($.configuration))));

  public static final JsonParser<Pair<Duration, SerializedActivity>> scheduledActivityP
      = productP
      . field("defer", durationP)
      . field("type", stringP)
      . optionalField("parameters", mapP(serializedValueP))
      . map(Iso.of(
          uncurry3(defer -> type -> parameters ->
              Pair.of(defer, new SerializedActivity(type, parameters.orElse(Collections.emptyMap())))),
          $ -> Uncurry.tuple3($.getLeft(), $.getRight().getTypeName(), Optional.of($.getRight().getParameters()))));

  public static final JsonParser<CreateSimulationMessage> createSimulationMessageP
      = productP
      . field("adaptationId", stringP)
      . field("startTime", timestampP.map(Iso.of(Timestamp::toInstant, Timestamp::new)))
      . field("samplingDuration", durationP)
      . field("activities", mapP(scheduledActivityP))
      . field("configuration", mapP(serializedValueP))
      . map(Iso.of(
          uncurry5(adaptationId -> startTime -> samplingDuration -> activities -> configuration ->
              new CreateSimulationMessage(adaptationId, startTime, samplingDuration, activities, configuration)),
          $ -> Uncurry.tuple5($.adaptationId(), $.startTime(), $.samplingDuration(), $.activityInstances(), $.configuration())));

  public static final JsonParser<Constraint> constraintP
      = productP
      . field("name", stringP)
      . field("summary", stringP)
      . field("description", stringP)
      . field("definition", stringP)
      . map(Iso.of(
          uncurry4(name -> summary -> description -> definition ->
              new Constraint(name, summary, description, definition)),
          $ -> Uncurry.tuple4($.name(), $.summary(), $.description(), $.definition())));
}
