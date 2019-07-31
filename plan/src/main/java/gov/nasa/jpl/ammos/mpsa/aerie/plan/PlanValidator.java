package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanValidator implements PlanValidatorInterface {
    private final String adaptationUri;

    public PlanValidator(final String adaptationUri) {
        this.adaptationUri = adaptationUri;
    }

    public Map<String, ActivityType> loadAdaptation(String adaptationId) {
        RestTemplate restTemplate = new RestTemplate();
        String uri = String.format("%s/%s/activities", adaptationUri, adaptationId);

        ResponseEntity<List<ActivityType>> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ActivityType>>() {
                        });

        // TODO: Check that the request succeeded

        List<ActivityType> typeList = response.getBody();
        Map<String, ActivityType> activityTypes = typeList
                .stream()
                .collect(Collectors.toMap(type -> type.getName(), type -> type));

        return activityTypes;
    }

    @Override
    public void validateActivitiesForPlan(PlanDetail plan) throws ValidationException {
        List<ActivityInstance> instanceList = plan.getActivityInstances();
        Map<String, ActivityType> activityTypeMap = loadAdaptation(plan.getAdaptationId());

        for (ActivityInstance activityInstance : instanceList) {
            String activityTypeName = activityInstance.getActivityType();
            if (!activityTypeMap.containsKey(activityTypeName)) {
                String reason = String.format(
                        "Activity type \"%s\" not defined for adaptation %s",
                        activityTypeName,
                        plan.getAdaptationId()
                );
                throw new ValidationException(reason);
            }
            validateActivity(activityInstance, activityTypeMap.get(activityTypeName));
        }
    }

    public void validateActivity(ActivityInstance instance, ActivityType type) throws ValidationException {
        List<ActivityInstanceParameter> instanceParameters = instance.getParameters();
        Map<String, ActivityTypeParameter> typeParameters = mapTypeParameters(type.getParameters());


        if (typeParameters.size() != instanceParameters.size()) {
            String reason = String.format(
                    "Activity Type \"%s\" contains %d parameters, but %d provided",
                    type.getName(),
                    typeParameters.size(),
                    instanceParameters.size()
            );
            throw new ValidationException(reason);
        }

        for (ActivityInstanceParameter instanceParameter : instanceParameters) {
            String parameterName = instanceParameter.getName();
            ActivityTypeParameter typeParameter = typeParameters.get(parameterName);

            if (typeParameter == null) {
                String reason = String.format(
                        "No parameter \"%s\" found for Activity Type \"%s\".",
                        parameterName,
                        type.getName()
                );
                throw new ValidationException(reason);
            }

            String parameterType = typeParameter.getType();
            validateParameter(parameterName, parameterType, instanceParameter.getValue());
        }
    }

    public void validateParameter(String parameterName, String parameterType, String value) throws ValidationException {
        switch (parameterType) {
            case "String":
                validateString(parameterName, value);
                break;

            case "Integer":
                validateInteger(parameterName, value);
                break;

            case "Double":
                validateDouble(parameterName, value);
                break;

            case "Boolean":
                validateBoolean(parameterName, value);
                break;

            case "Map":
                validateMap(parameterName, value);
                break;

            case "List":
                validateList(parameterName, value);
                break;

            default:
                throw new ValidationException(String.format(
                        "Unexpected parameter type \"%s\" for parameter \"%s\"",
                        parameterType,
                        parameterName
                ));
        }
    }

    public void validateString(String name, String value) {
        // nothing to do
    }

    public void validateInteger(String name, String value) throws ValidationException {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String reason = String.format(
                    "Parameter \"%s\" should be type \"Integer\" but is not parsable into that type",
                    name
            );
            throw new ValidationException(reason);
        }
    }

    public void validateDouble(String name, String value) throws ValidationException {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            String reason = String.format(
                    "Parameter \"%s\" should be type \"Double\" but is not parsable into that type",
                    name
            );
            throw new ValidationException(reason);
        }
    }

    public void validateBoolean(String name, String value) throws ValidationException {
        List<String> validValues = new ArrayList<>();
        validValues.add("true");
        validValues.add("false");
        if (!validValues.contains(value.toLowerCase())) {
            String reason = String.format(
                    "Parameter \"%s\" should be type \"Boolean \" but is not parsable into that type",
                    name
            );
            throw new ValidationException(reason);
        }
    }

    public void validateMap(String name, String value) throws ValidationException {
        // TODO: Implement me
    }

    public void validateList(String name, String value) throws ValidationException {
        // TODO: Implement me
    }

    public Map<String, ActivityTypeParameter> mapTypeParameters(List<ActivityTypeParameter> list) {
        Map<String, ActivityTypeParameter> map = new HashMap<>();
        for (ActivityTypeParameter parameter : list) {
            map.put(parameter.getName(), parameter);
        }
        return map;
    }

}
