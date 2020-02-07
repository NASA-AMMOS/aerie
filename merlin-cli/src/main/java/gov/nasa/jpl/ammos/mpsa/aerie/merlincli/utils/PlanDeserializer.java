package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidEntityException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;

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

public final class PlanDeserializer {
    private PlanDeserializer() {}

    public static String deserializeString(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonString)) throw new InvalidEntityException();
        final JsonString stringJson = (JsonString)jsonValue;

        return stringJson.getString();
    }

    public static List<ActivityInstanceParameter> deserializeActivityParameterMap(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject parameterMapJson = (JsonObject)jsonValue;

        final List<ActivityInstanceParameter> parameters = new ArrayList<>();
        for (final var entry : parameterMapJson.entrySet()) {
            parameters.add(new ActivityInstanceParameter(entry.getKey(), entry.getValue().toString()));
        }

        return parameters;
    }

    public static ActivityInstance deserializeActivityInstance(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject activityInstanceJson = (JsonObject)jsonValue;

        Optional<String> type = Optional.empty();
        Optional<String> startTimestamp = Optional.empty();
        Optional<List<ActivityInstanceParameter>> parameters = Optional.empty();

        for (final var entry : activityInstanceJson.entrySet()) {
            final JsonValue entryValue = entry.getValue();

            switch (entry.getKey()) {
                case "activityType":
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
                    continue;
                    //throw new InvalidEntityException();
            }
        }

        final ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivityType(type.orElseThrow(InvalidEntityException::new));
        activityInstance.setStartTimestamp(startTimestamp.orElseThrow(InvalidEntityException::new));
        activityInstance.setParameters(parameters.orElseGet(ArrayList::new));

        return activityInstance;
    }

    public static ActivityInstance deserializeActivityInstancePatch(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject activityInstanceJson = (JsonObject)jsonValue;

        Optional<String> type = Optional.empty();
        Optional<String> startTimestamp = Optional.empty();
        Optional<List<ActivityInstanceParameter>> parameters = Optional.empty();

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
        activityInstance.setActivityType(type.orElse(null));
        activityInstance.setStartTimestamp(startTimestamp.orElse(null));
        activityInstance.setParameters(parameters.orElse(null));

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

    public static Map<String, ActivityInstance> deserializeActivityInstanceMap(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject activityInstanceMapJson = (JsonObject)jsonValue;

        final Map<String, ActivityInstance> activityInstances = new HashMap<>();
        for (final var entry : activityInstanceMapJson.entrySet()) {
            activityInstances.put(entry.getKey(), deserializeActivityInstance(entry.getValue()));
        }

        return activityInstances;
    }

    public static List<ActivityInstance> deserializeActivityInstanceMapPatch(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject activityInstanceMapJson = (JsonObject)jsonValue;

        final List<ActivityInstance> activityInstances = new ArrayList<>();
        for (final var entry : activityInstanceMapJson.entrySet()) {
            if (entry.getValue() == JsonValue.NULL) {
                activityInstances.add(null);
            } else {
                activityInstances.add(deserializeActivityInstance(entry.getValue()));
            }
        }

        return activityInstances;
    }

    public static PlanDetail deserializePlanPatch(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject newPlanJson = (JsonObject)jsonValue;

        Optional<String> name = Optional.empty();
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

        final PlanDetail plan = new PlanDetail();
        plan.setName(name.orElse(null));
        plan.setStartTimestamp(startTimestamp.orElse(null));
        plan.setEndTimestamp(endTimestamp.orElse(null));
        plan.setActivityInstances(activityInstances.orElse(null));

        return plan;
    }

    public static PlanDetail deserializePlan(final JsonValue jsonValue) throws InvalidEntityException {
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

        final PlanDetail newPlan = new PlanDetail();
        newPlan.setName(name.orElseThrow(InvalidEntityException::new));
        newPlan.setAdaptationId(adaptationId.orElseThrow(InvalidEntityException::new));
        newPlan.setStartTimestamp(startTimestamp.orElseThrow(InvalidEntityException::new));
        newPlan.setEndTimestamp(endTimestamp.orElseThrow(InvalidEntityException::new));
        newPlan.setActivityInstances(activityInstances.orElseGet(ArrayList::new));

        return newPlan;
    }
}
