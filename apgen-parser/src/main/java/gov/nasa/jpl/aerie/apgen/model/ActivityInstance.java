package gov.nasa.jpl.aerie.apgen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActivityInstance {
    private final String type;
    private final String name;
    private final String id;
    private final List<Attribute> attributes;
    private final List<ActivityInstanceParameter> parameters;

    public ActivityInstance(final String type, final String name, final String id) {
        this.type = type;
        this.name = name;
        this.id = id;
        this.attributes = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public List<Attribute> getAttributes() {
        return List.copyOf(this.attributes);
    }

    public Attribute getAttribute(final String name) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(name)) return attribute;
        }
        return null;
    }

    public void addAttribute(final Attribute attribute) {
        Objects.requireNonNull(attribute);
        this.attributes.add(attribute);
    }

    public boolean hasAttribute(final String name) {
        return getAttribute(name) != null;
    }

    public List<ActivityInstanceParameter> getParameters() {
        return List.copyOf(this.parameters);
    }

    public ActivityInstanceParameter getParameter(final String name) {
        for (ActivityInstanceParameter parameter : parameters) {
            if (parameter.getName().equals(name)) return parameter;
        }
        return null;
    }

    public void addParameter(final ActivityInstanceParameter parameter) {
        Objects.requireNonNull(parameter);
        this.parameters.add(parameter);
    }

    public boolean hasParameter(final String name) {
        return getParameter(name) != null;
    }
}
