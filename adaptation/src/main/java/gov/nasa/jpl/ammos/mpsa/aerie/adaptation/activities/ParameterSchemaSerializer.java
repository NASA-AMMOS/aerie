package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.activities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.lang.reflect.Type;

public class ParameterSchemaSerializer implements JsonSerializer<ParameterSchema> {
    @Override
    public JsonElement serialize(ParameterSchema parameterSchema, Type type, JsonSerializationContext jsonSerializationContext) {
        return null;
    }
}