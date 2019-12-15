package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RequestDeserializers {
  public static String deserializeString(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonString)) throw new InvalidEntityException();
    final JsonString stringJson = (JsonString)jsonValue;

    return stringJson.getString();
  }

  public static SerializedParameter deserializeActivityParameter(final JsonValue jsonValue) throws InvalidEntityException {
    if (jsonValue == JsonValue.NULL) {
      return SerializedParameter.NULL;
    } else if (jsonValue == JsonValue.FALSE) {
      return SerializedParameter.of(false);
    } else if (jsonValue == JsonValue.TRUE) {
      return SerializedParameter.of(true);
    } else if (jsonValue instanceof JsonString) {
      return SerializedParameter.of(deserializeString(jsonValue));
    } else if (jsonValue instanceof JsonNumber) {
      if (((JsonNumber)jsonValue).isIntegral()) {
        return SerializedParameter.of(((JsonNumber)jsonValue).intValueExact());
      } else {
        return SerializedParameter.of(((JsonNumber)jsonValue).doubleValue());
      }
    } else if (jsonValue instanceof JsonArray) {
      return SerializedParameter.of(deserializeActivityParameterList(jsonValue));
    } else if (jsonValue instanceof JsonObject) {
      return SerializedParameter.of(deserializeActivityParameterMap(jsonValue));
    } else {
      throw new InvalidEntityException();
    }
  }

  public static List<SerializedParameter> deserializeActivityParameterList(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonArray)) throw new InvalidEntityException();
    final JsonArray parameterMapJson = (JsonArray)jsonValue;

    final List<SerializedParameter> parameters = new ArrayList<>();
    for (final var item : parameterMapJson) {
      parameters.add(deserializeActivityParameter(item));
    }

    return parameters;
  }

  public static Map<String, SerializedParameter> deserializeActivityParameterMap(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
    final JsonObject parameterMapJson = (JsonObject)jsonValue;

    final Map<String, SerializedParameter> parameters = new HashMap<>();
    for (final var entry : parameterMapJson.entrySet()) {
      parameters.put(entry.getKey(), deserializeActivityParameter(entry.getValue()));
    }

    return parameters;
  }
}
