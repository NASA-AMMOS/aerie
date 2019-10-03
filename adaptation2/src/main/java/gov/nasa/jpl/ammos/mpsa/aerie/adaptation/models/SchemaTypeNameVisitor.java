package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.util.Map;

public final class SchemaTypeNameVisitor implements ParameterSchema.Visitor<String> {
    public String onDouble() {
        return "Double";
    }

    public String onInt() {
        return "Integer";
    }

    public String onBoolean() {
        return "Boolean";
    }

    public String onString() {
        return "String";
    }

    public String onList(final ParameterSchema value) {
        return "List";
    }

    public String onMap(final Map<String, ParameterSchema> value) {
        return "Map";
    }
}
