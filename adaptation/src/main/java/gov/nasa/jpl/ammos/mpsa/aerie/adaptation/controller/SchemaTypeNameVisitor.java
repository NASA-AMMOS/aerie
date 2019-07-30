package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.util.Map;

public class SchemaTypeNameVisitor implements ParameterSchema.Visitor<String> {
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

    public String onList(ParameterSchema value) {
        return "List";
    }

    public String onMap(Map<String, ParameterSchema> value) {
        return "Map";
    }
}
