package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

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
