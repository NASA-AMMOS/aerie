package gov.nasa.jpl.aerie.services.plan.http;

import java.util.List;

public class NewAdaptationValidationException extends Exception {
    private final List<String> errors;

    public NewAdaptationValidationException(final String message, final List<String> errors) {
        super(message + ": " + errors.toString());
        this.errors = List.copyOf(errors);
    }

    public List<String> getValidationErrors() {
        return this.errors;
    }
}
