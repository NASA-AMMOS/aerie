package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions;

/**
 * Exception defined for invalid tokens when parsing input for
 * activity updates in the Merlin CLI.
 */
public class InvalidTokenException extends Exception {

    private String token;

    public InvalidTokenException(String token, String message) {
        super(message);
        this.token = token;
    }

    public InvalidTokenException(String token, String message, Throwable cause) {
        super(message, cause);
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }
}
