package gov.nasa.jpl.aerie.services.plan.remotes;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.SimulationResults;
import gov.nasa.jpl.aerie.services.plan.utils.HttpRequester;
import gov.nasa.jpl.aerie.time.Duration;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.boolP;
import static gov.nasa.jpl.aerie.json.BasicParsers.chooseP;
import static gov.nasa.jpl.aerie.json.BasicParsers.doubleP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.nullP;
import static gov.nasa.jpl.aerie.json.BasicParsers.recursiveP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

public final class RemoteAdaptationService implements AdaptationService {
  private final HttpRequester client;

  public RemoteAdaptationService(final URI serviceBaseUri) {
    this.client = new HttpRequester(HttpClient.newHttpClient(), serviceBaseUri);
  }

  @Override
  public boolean isMissionModelDefined(final String adaptationId) {
    final HttpResponse<String> response;
    try {
      response = this.client.sendRequest("HEAD", "/adaptations/" + adaptationId);
    } catch (final IOException | InterruptedException ex) {
      throw new AdaptationAccessException(ex);
    }

    switch (response.statusCode()) {
      case 200:
        return true;

      case 404:
        return false;

      default:
        throw new AdaptationAccessException("unexpected status code `" + response.statusCode() + "`");
    }
  }

  @Override
  public List<String> areActivityParametersValid(final String adaptationId, final SerializedActivity activityParameters) throws NoSuchAdaptationException {
    final HttpResponse<String> response;
    try {
      final var requestPath = String.format("/adaptations/%s/activities/%s/validate", adaptationId, activityParameters.getTypeName());
      final var parameters = serializeActivityParameterMap(activityParameters.getParameters());

      response = this.client.sendRequest("POST", requestPath, parameters);
    } catch (final IOException | InterruptedException ex) {
      throw new AdaptationAccessException(ex);
    }

    switch (response.statusCode()) {
      case 200:
        final JsonValue responseJson = Json.createReader(new StringReader(response.body())).readValue();
        return deserializeActivityValidation(responseJson);

      case 404:
        throw new NoSuchAdaptationException();

      default:
        throw new AdaptationAccessException("unexpected status code `" + response.statusCode() + "`: " + response.body());
    }
  }

  @Override
  public SimulationResults simulatePlan(final Plan plan, final long samplingPeriod) throws NoSuchAdaptationException {
    final var startTime = plan.startTimestamp.toInstant();

    final HttpResponse<String> response;
    try {
      final var samplingDuration = startTime.until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS);

      final var requestBody = Json.createObjectBuilder()
          .add("adaptationId", plan.adaptationId)
          .add("startTime", plan.startTimestamp.toString())
          .add("samplingDuration", samplingDuration)
          .add("samplingPeriod", samplingPeriod)
          .add("activities", serializeScheduledActivities(plan))
          .build();

      response = this.client.sendRequest("POST", "/simulations", requestBody);
    } catch (final IOException | InterruptedException ex) {
      throw new AdaptationAccessException(ex);
    }

    switch (response.statusCode()) {
      case 200: {
        final var responseJson = Json.createReader(new StringReader(response.body())).readValue();

        return deserializeSimulationResults(startTime, responseJson);
      }

      case 404:
        throw new NoSuchAdaptationException();

      case 400: {
        final var responseJson = Json.createReader(new StringReader(response.body())).readObject();
        switch (responseJson.getString("kind")) {
          case "invalid-json":
            throw new RuntimeException("Internal error -- illegal JSON rejected by remote adaptation service");
          case "invalid-entity":
            throw new RuntimeException("API mismatch -- remote adaptation service rejected generated JSON");
          case "invalid-activities":
            throw new RuntimeException("Adaptation mismatch -- remote adaptation service rejected activity instances");
          default:
            throw new RuntimeException("Unknown error kind returned by remote adaptation service: " + responseJson.getString("kind") + ".\n\tresponse entity: " + responseJson);
        }
      }

      default:
        throw new AdaptationAccessException("unexpected status code `" + response.statusCode() + "`: " + response.body());
    }
  }

  private static JsonParser<Duration> durationP
      = longP
      .map(v -> Duration.of(v, Duration.MICROSECONDS));

  private static JsonParser<SerializedValue> serializedValueP =
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

  private static JsonParser<List<Duration>> timestampsP = listP(durationP);
  private static JsonParser<Map<String, List<SerializedValue>>> timelinesP = mapP(listP(serializedValueP));

  public static SimulationResults deserializeSimulationResults(final Instant startTime, final JsonValue json) {
    if (!(json instanceof JsonObject)) throw new InvalidServiceResponseException();
    final var jsonObject = (JsonObject) json;

    final var timestamps = timestampsP.parse(jsonObject.get("times")).getSuccessOrThrow(InvalidServiceResponseException::new);
    final var timelines = timelinesP.parse(jsonObject.get("resources")).getSuccessOrThrow(InvalidServiceResponseException::new);
    final var constraints = jsonObject.get("constraints");
    final var activities = jsonObject.get("activities");

    return new SimulationResults(startTime, timestamps, timelines, constraints, activities);
  }

  private static List<String> deserializeActivityValidation(final JsonValue json) {
    if (!(json instanceof JsonObject)) throw new InvalidServiceResponseException();

    List<String> failures = List.of();
    for (final var entry : ((JsonObject)json).entrySet()) {
      switch (entry.getKey()) {
        case "instantiable":
          break;
        case "failures":
          failures = deserializeParameterFailures(entry.getValue());
          break;
        default:
          throw new InvalidServiceResponseException();
      }
    }

    return failures;
  }

  private static List<String> deserializeParameterFailures(final JsonValue json) {
    if (!(json instanceof JsonArray)) throw new InvalidServiceResponseException();

    final var parameterFailures = new ArrayList<String>();
    for (final var parameterFailureJson : (JsonArray)json) {
      if (!(parameterFailureJson instanceof JsonString)) throw new InvalidServiceResponseException();
      parameterFailures.add(((JsonString)parameterFailureJson).getString());
    }

    return parameterFailures;
  }

  private JsonValue serializeActivityParameter(final SerializedValue parameter) {
    return parameter.match(new SerializedValue.Visitor<>() {
      @Override
      public JsonValue onNull() {
        return JsonValue.NULL;
      }

      @Override
      public JsonValue onReal(final double value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onInt(final long value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onBoolean(final boolean value) {
        return (value) ? JsonValue.TRUE : JsonValue.FALSE;
      }

      @Override
      public JsonValue onString(final String value) {
        return Json.createValue(value);
      }

      @Override
      public JsonValue onMap(final Map<String, SerializedValue> value) {
        return serializeActivityParameterMap(value);
      }

      @Override
      public JsonValue onList(final List<SerializedValue> value) {
        return serializeActivityParameterList(value);
      }
    });
  }

  private JsonValue serializeActivityParameterMap(final Map<String, SerializedValue> parameterMap) {
    final var parameters = Json.createObjectBuilder();
    for (final var entry : parameterMap.entrySet()) {
      parameters.add(entry.getKey(), serializeActivityParameter(entry.getValue()));
    }
    return parameters.build();
  }

  private JsonValue serializeActivityParameterList(final List<SerializedValue> parameterList) {
    final var parameters = Json.createArrayBuilder();
    for (final var parameter : parameterList) {
      parameters.add(serializeActivityParameter(parameter));
    }
    return parameters.build();
  }

  private JsonValue serializeScheduledActivities(final Plan plan) {
    final var startTime = plan.startTimestamp.toInstant();

    final var scheduledActivities = Json.createObjectBuilder();
    for (final var entry : plan.activityInstances.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();
      scheduledActivities.add(id, Json.createObjectBuilder()
              .add("defer", startTime.until(activity.startTimestamp.toInstant(), ChronoUnit.MICROS))
              .add("type", activity.type)
              .add("parameters", serializeActivityParameterMap(activity.parameters))
              .build());
    }

    return scheduledActivities.build();
  }

  public static class InvalidServiceResponseException extends RuntimeException {
  }

  private static class AdaptationAccessException extends RuntimeException {
    public AdaptationAccessException(final Throwable cause) {
      super(cause);
    }

    public AdaptationAccessException(final String message) {
      super(message);
    }
  }
}
