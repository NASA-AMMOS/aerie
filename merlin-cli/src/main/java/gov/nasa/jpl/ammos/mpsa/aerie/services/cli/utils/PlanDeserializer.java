package gov.nasa.jpl.ammos.mpsa.aerie.services.cli.utils;

import gov.nasa.jpl.ammos.mpsa.aerie.services.cli.exceptions.InvalidEntityException;
import gov.nasa.jpl.ammos.mpsa.aerie.services.cli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.services.cli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlanDeserializer {
    private PlanDeserializer() {}

    public static SerializedValue deserializeActivityParameter(JsonValue jsonValue) throws InvalidEntityException {
        if (jsonValue == JsonValue.NULL) {
            return SerializedValue.NULL;
        } else if (jsonValue == JsonValue.FALSE) {
            return SerializedValue.of(false);
        } else if (jsonValue == JsonValue.TRUE) {
            return SerializedValue.of(true);
        } else if (jsonValue instanceof JsonString) {
            return SerializedValue.of(deserializeString(jsonValue));
        } else if (jsonValue instanceof JsonNumber) {
            if (((JsonNumber)jsonValue).isIntegral()) {
                return SerializedValue.of(((JsonNumber)jsonValue).intValueExact());
            } else {
                return SerializedValue.of(((JsonNumber)jsonValue).doubleValue());
            }
        } else if (jsonValue instanceof JsonArray) {
            return SerializedValue.of(deserializeActivityParameterList(jsonValue));
        } else if (jsonValue instanceof JsonObject) {
            return SerializedValue.of(deserializeActivityParameterMap(jsonValue));
        } else {
            throw new InvalidEntityException();
        }
    }

    public static List<SerializedValue> deserializeActivityParameterList(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonArray)) throw new InvalidEntityException();
        final JsonArray parameterMapJson = (JsonArray)jsonValue;

        final List<SerializedValue> parameters = new ArrayList<>();
        for (final var item : parameterMapJson) {
            parameters.add(deserializeActivityParameter(item));
        }

        return parameters;
    }

    public static String deserializeString(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonString)) throw new InvalidEntityException();
        final JsonString stringJson = (JsonString)jsonValue;

        return stringJson.getString();
    }

    public static Instant deserializeTimestamp(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonString)) throw new InvalidEntityException();
        return deserializeTimestamp(((JsonString)jsonValue).getString());
    }

    public static Instant deserializeTimestamp(final String str) throws InvalidEntityException {
        try {
            return Instant.from(
                DateTimeFormatter
                    .ofPattern("uuuu-DDD'T'HH:mm:ss[.nnnnnnnnn]")
                    .withZone(ZoneOffset.UTC)
                    .parse(str));
        } catch (DateTimeParseException e) {
            throw new InvalidEntityException(e);
        }
    }

    public static Map<String, SerializedValue> deserializeActivityParameterMap(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject parameterMapJson = (JsonObject)jsonValue;

        final Map<String, SerializedValue> parameters = new HashMap<>();
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
        Optional<Map<String, SerializedValue>> parameters = Optional.empty();

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
        activityInstance.setParameters(parameters.orElseGet(HashMap::new));

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

    public static PlanDetail deserializePlan(final JsonValue jsonValue) throws InvalidEntityException {
        if (!(jsonValue instanceof JsonObject)) throw new InvalidEntityException();
        final JsonObject newPlanJson = (JsonObject)jsonValue;

        Optional<String> name = Optional.empty();
        Optional<String> adaptationId = Optional.empty();
        Optional<Instant> startTimestamp = Optional.empty();
        Optional<Instant> endTimestamp = Optional.empty();
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
                    startTimestamp = Optional.of(deserializeTimestamp(entryValue));
                    break;

                case "endTimestamp":
                    if (entryValue == JsonValue.NULL) throw new InvalidEntityException();
                    endTimestamp = Optional.of(deserializeTimestamp(entryValue));
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
