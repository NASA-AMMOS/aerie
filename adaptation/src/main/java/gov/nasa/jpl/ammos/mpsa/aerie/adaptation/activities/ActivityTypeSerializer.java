package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.activities;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ActivityTypeSerializer implements JsonSerializer<ActivityType> {

    @Override
    public JsonElement serialize(ActivityType activityType, Type type, JsonSerializationContext jsonSerializationContext) {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        List<JsonObject> nodes = new ArrayList<>();

        json.addProperty("activityClass", activityType.getName());

        activityType.getParameters().forEach((p) -> {
            JsonObject node = new JsonObject();
            node.addProperty("name", p.getName());
            node.addProperty("type", p.getType().toString().replaceFirst("class ", ""));
            node.addProperty("value", p.getValue().toString());
            nodes.add(node);
        });

        JsonElement nodesElement = gson.toJsonTree(nodes);
        json.add("parameters", nodesElement);

        return json;
    }
}