package gov.nasa.jpl.ammos.mpsa.apgen.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class ActivityType {
    private final String name;
    private final List<Attribute> attributes;
    private final List<ActivityTypeParameter> parameters;

    public ActivityType(final String name) {
        this.name = name;
        this.attributes = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

    public String getName() {
        return this.name;
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

    public List<ActivityTypeParameter> getParameters() {
        return List.copyOf(this.parameters);
    }

    public ActivityTypeParameter getParameter(final String name) {
        for (ActivityTypeParameter parameter : parameters) {
            if (parameter.getName().equals(name)) return parameter;
        }
        return null;
    }

    public void addParameter(final ActivityTypeParameter parameter) {
        Objects.requireNonNull(parameter);
        this.parameters.add(parameter);
    }

    public boolean hasParameter(final String name) {
        return getParameter(name) != null;
    }
}
