package gov.nasa.jpl.adaptation.activities;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ActivityTypeSerializer implements JsonSerializer<ActivityType> {

    @Override
    public JsonElement serialize(ActivityType activityType, Type type, JsonSerializationContext jsonSerializationContext) {
        Map<String, Object> activityTypeMap = new HashMap<>();
        activityTypeMap.put("activityClass", activityType.getActivityClass().toString());
        activityTypeMap.put("parameters", activityType.getParameters());
        activityTypeMap.put("listeners", activityType.getListeners());
        System.out.println(activityTypeMap);

        Gson gson = new Gson();
        return gson.toJsonTree(activityTypeMap);
    }
}