package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

// TODO: Use schemas.ActivityTypeParameter, add readOnly and value fields
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;

public class ParameterBuilder {

    private Parameter _parameter;

    public ParameterBuilder() {
        _parameter = new Parameter();
    }

    public ParameterBuilder withName(String name) {
        _parameter.setName(name);
        return this;
    }

    public ParameterBuilder ofType(Object type) {
        _parameter.setType(type);
        return this;
    }

    public ParameterBuilder withValue(Object value) {
        _parameter.setValue(value);
        return this;
    }

    public ParameterBuilder asReadOnly(boolean readOnly) {
        _parameter.setReadOnly(readOnly);
        return this;
    }

    public Parameter getParameter() {
        return _parameter;
    }
}