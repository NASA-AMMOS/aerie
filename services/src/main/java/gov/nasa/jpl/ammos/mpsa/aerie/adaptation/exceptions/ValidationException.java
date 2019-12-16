package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

import java.util.List;

public class ValidationException extends Exception {
    private final List<String> errors;

    public ValidationException(final String message, final List<String> errors) {
        super(message + ": " + errors.toString());
        this.errors = List.copyOf(errors);
    }

    public List<String> getValidationErrors() {
        return this.errors;
    }
}
