package gov.nasa.jpl.aerie.services.cli.exceptions;

/**
 * Exception defined for argument lists of invalid length
 * in the Merlin CLI.
 */
public class InvalidNumberOfArgsException extends Exception {

    public InvalidNumberOfArgsException(String message) {
        super(message);
    }

    public InvalidNumberOfArgsException(String message, Throwable cause) {
        super(message, cause);
    }
}
