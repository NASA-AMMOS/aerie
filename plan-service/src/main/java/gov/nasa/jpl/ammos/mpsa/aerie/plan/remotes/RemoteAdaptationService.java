package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.InvalidEntityException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.http.RequestDeserializers;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.utils.HttpRequester;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final var startTime = timestampToInstant(plan.startTimestamp);

    final HttpResponse<String> response;
    try {
      final var samplingDuration = startTime.until(timestampToInstant(plan.endTimestamp), ChronoUnit.MICROS);

      final var requestBody = Json.createObjectBuilder()
          .add("adaptationId", plan.adaptationId)
          .add("startTime", plan.startTimestamp)
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

  private static SimulationResults deserializeSimulationResults(final Instant startTime, final JsonValue json) {
    if (!(json instanceof JsonObject)) throw new InvalidServiceResponseException();
    final var jsonObject = (JsonObject) json;

    final var timestamps = deserializeTimestamps(jsonObject.get("times"));
    final var timelines = deserializeTimelines(jsonObject.get("resources"));
    final var constraints = jsonObject.get("constraints");

    return new SimulationResults(startTime, timestamps, timelines, constraints);
  }

  private static List<Duration> deserializeTimestamps(final JsonValue json) {
    if (!(json instanceof JsonArray)) throw new InvalidServiceResponseException();
    final var jsonArray = (JsonArray) json;

    final var timestamps = new ArrayList<Duration>(jsonArray.size());
    for (final var item : jsonArray) {
      final var timestamp = Duration.of(((JsonNumber) item).longValue(), Duration.MICROSECONDS);
      timestamps.add(timestamp);
    }

    return timestamps;
  }

  private static Map<String, List<SerializedParameter>> deserializeTimelines(final JsonValue json) {
    if (!(json instanceof JsonObject)) throw new InvalidServiceResponseException();
    final var jsonObject = (JsonObject) json;

    final var timelines = new HashMap<String, List<SerializedParameter>>(jsonObject.size());
    for (final var entry : jsonObject.entrySet()) {
      timelines.put(entry.getKey(), deserializeTimeline(entry.getValue()));
    }

    return timelines;
  }

  private static List<SerializedParameter> deserializeTimeline(final JsonValue json) {
    if (!(json instanceof JsonArray)) throw new InvalidServiceResponseException();
    final var jsonArray = (JsonArray) json;

    final var timeline = new ArrayList<SerializedParameter>(jsonArray.size());
    for (final var item : jsonArray) {
      try {
        timeline.add(RequestDeserializers.deserializeActivityParameter(item));
      } catch (final InvalidEntityException ex) {
        throw new InvalidServiceResponseException();
      }
    }

    return timeline;
  }

  private static Instant timestampToInstant(final String timestamp) {
    // TODO: handle parse errors
    final var format = DateTimeFormatter.ofPattern("uuuu-DDD'T'HH:mm:ss[.n]");
    return LocalDateTime.parse(timestamp, format).atZone(ZoneOffset.UTC).toInstant();
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

  private JsonValue serializeActivityParameter(final SerializedParameter parameter) {
    return parameter.match(new SerializedParameter.Visitor<>() {
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
      public JsonValue onMap(final Map<String, SerializedParameter> value) {
        return serializeActivityParameterMap(value);
      }

      @Override
      public JsonValue onList(final List<SerializedParameter> value) {
        return serializeActivityParameterList(value);
      }
    });
  }

  private JsonValue serializeActivityParameterMap(final Map<String, SerializedParameter> parameterMap) {
    final var parameters = Json.createObjectBuilder();
    for (final var entry : parameterMap.entrySet()) {
      parameters.add(entry.getKey(), serializeActivityParameter(entry.getValue()));
    }
    return parameters.build();
  }

  private JsonValue serializeActivityParameterList(final List<SerializedParameter> parameterList) {
    final var parameters = Json.createArrayBuilder();
    for (final var parameter : parameterList) {
      parameters.add(serializeActivityParameter(parameter));
    }
    return parameters.build();
  }

  private JsonValue serializeScheduledActivities(final Plan plan) {
    final var startTime = timestampToInstant(plan.startTimestamp);

    final var scheduledActivities = Json.createObjectBuilder();
    for (final var entry : plan.activityInstances.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();
      scheduledActivities.add(id, Json.createObjectBuilder()
              .add("defer", startTime.until(timestampToInstant(activity.startTimestamp), ChronoUnit.MICROS))
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
