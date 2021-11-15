package gov.nasa.jpl.aerie.merlin.server.http;

import java.util.List;

public class NewMissionModelValidationException extends Exception {
    private final List<String> errors;

    public NewMissionModelValidationException(final String message, final List<String> errors) {
        super(message + ": " + errors.toString());
        this.errors = List.copyOf(errors);
    }

    public List<String> getValidationErrors() {
        return this.errors;
    }
}
