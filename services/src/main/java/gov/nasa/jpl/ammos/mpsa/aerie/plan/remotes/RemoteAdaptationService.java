package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.utils.HttpRequester;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
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
      response = this.client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities");
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
