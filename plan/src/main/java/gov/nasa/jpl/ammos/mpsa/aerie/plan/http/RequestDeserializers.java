package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RequestDeserializers {
  private RequestDeserializers() {}

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

  public static ActivityInstance deserializeActivityInstance(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
    final JsonObject activityInstanceJson = (JsonObject)jsonValue;

    Optional<String> type = Optional.empty();
    Optional<String> startTimestamp = Optional.empty();
    Optional<Map<String, SerializedParameter>> parameters = Optional.empty();

    for (final var entry : activityInstanceJson.entrySet()) {
      final JsonValue entryValue = entry.getValue();

      switch (entry.getKey()) {
        case "type":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          type = Optional.of(deserializeString(entryValue));
          break;

        case "startTimestamp":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          startTimestamp = Optional.of(deserializeString(entryValue));
          break;

        case "parameters":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          parameters = Optional.of(deserializeActivityParameterMap(entryValue));
          break;

        default:
          throw new InvalidEntityException();
      }
    }

    final ActivityInstance activityInstance = new ActivityInstance();
    activityInstance.type = type.orElseThrow(InvalidEntityException::new);
    activityInstance.startTimestamp = startTimestamp.orElseThrow(InvalidEntityException::new);;
    activityInstance.parameters = parameters.orElseGet(HashMap::new);

    return activityInstance;
  }

  public static List<ActivityInstance> deserializeActivityInstanceList(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonArray)) throw new InvalidEntityException();
    final JsonArray activityInstanceListJson = (JsonArray)jsonValue;

    final List<ActivityInstance> activityInstances = new ArrayList<>();
    for (final var itemJson : activityInstanceListJson) {
      if (itemJson == JsonValue.NULL) throw new InvalidEntityException();
      activityInstances.add(deserializeActivityInstance(itemJson));
    }

    return activityInstances;
  }

  public static Map<String, ActivityInstance> deserializeActivityInstanceMapPatch(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
    final JsonObject activityInstanceMapJson = (JsonObject)jsonValue;

    final Map<String, ActivityInstance> activityInstances = new HashMap<>();
    for (final var entry : activityInstanceMapJson.entrySet()) {
      if (entry.getValue() == JsonValue.NULL) {
        activityInstances.put(entry.getKey(), null);
      } else {
        activityInstances.put(entry.getKey(), deserializeActivityInstance(entry.getValue()));
      }
    }

    return activityInstances;
  }

  public static Plan deserializePlanPatch(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
    final JsonObject newPlanJson = (JsonObject)jsonValue;

    Optional<String> name = Optional.empty();
    Optional<String> adaptationId = Optional.empty();
    Optional<String> startTimestamp = Optional.empty();
    Optional<String> endTimestamp = Optional.empty();
    Optional<Map<String, ActivityInstance>> activityInstances = Optional.empty();

    for (final var entry : newPlanJson.entrySet()) {
      final JsonValue entryValue = entry.getValue();

      switch (entry.getKey()) {
        case "name":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          name = Optional.of(deserializeString(entryValue));
          break;

        case "adaptationId":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          adaptationId = Optional.of(deserializeString(entryValue));
          break;

        case "startTimestamp":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          startTimestamp = Optional.of(deserializeString(entryValue));
          break;

        case "endTimestamp":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          endTimestamp = Optional.of(deserializeString(entryValue));
          break;

        case "activityInstances":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          activityInstances = Optional.of(deserializeActivityInstanceMapPatch(entryValue));
          break;

        default:
          throw new InvalidEntityException();
      }
    }

    final Plan plan = new Plan();
    plan.name = name.orElse(null);
    plan.adaptationId = adaptationId.orElse(null);
    plan.startTimestamp = startTimestamp.orElse(null);
    plan.endTimestamp = endTimestamp.orElse(null);
    plan.activityInstances = activityInstances.orElse(null);

    return plan;
  }

  public static NewPlan deserializeNewPlan(final JsonValue jsonValue) throws InvalidEntityException {
    if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
    final JsonObject newPlanJson = (JsonObject)jsonValue;

    Optional<String> name = Optional.empty();
    Optional<String> adaptationId = Optional.empty();
    Optional<String> startTimestamp = Optional.empty();
    Optional<String> endTimestamp = Optional.empty();
    Optional<List<ActivityInstance>> activityInstances = Optional.empty();

    for (final var entry : newPlanJson.entrySet()) {
      final JsonValue entryValue = entry.getValue();

      switch (entry.getKey()) {
        case "name":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          name = Optional.of(deserializeString(entryValue));
          break;

        case "adaptationId":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          adaptationId = Optional.of(deserializeString(entryValue));
          break;

        case "startTimestamp":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          startTimestamp = Optional.of(deserializeString(entryValue));
          break;

        case "endTimestamp":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          endTimestamp = Optional.of(deserializeString(entryValue));
          break;

        case "activityInstances":
          if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
          activityInstances = Optional.of(deserializeActivityInstanceList(entryValue));
          break;

        default:
          throw new InvalidEntityException();
      }
    }

    final NewPlan newPlan = new NewPlan();
    newPlan.name = name.orElseThrow(InvalidEntityException::new);
    newPlan.adaptationId = adaptationId.orElseThrow(InvalidEntityException::new);
    newPlan.startTimestamp = startTimestamp.orElseThrow(InvalidEntityException::new);
    newPlan.endTimestamp = endTimestamp.orElseThrow(InvalidEntityException::new);
    newPlan.activityInstances = activityInstances.orElseGet(ArrayList::new);

    return newPlan;
  }
}
