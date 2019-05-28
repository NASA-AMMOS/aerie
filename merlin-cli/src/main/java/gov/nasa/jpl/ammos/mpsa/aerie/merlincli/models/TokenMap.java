package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;

public class TokenMap {
    private String name;
    private String value;
    private String token;

    public TokenMap(String name, String value, String token) {
        this.name = name;
        this.value = value;
        this.token = token;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public String getToken() {
        return this.token;
    }

    public String toString() {
        return this.token;
    }

    /**
     * Parse a token of the form <name>=<value> into a TokenMap
     * @param token
     * @return TokenMap representation of the token
     * @throws InvalidTokenException
     */
    public static TokenMap parseToken(String token) throws InvalidTokenException {
        String[] components = token.split("=", 2);
        if (components.length < 2) {
            throw new InvalidTokenException(token, "Token must have format <name>=<value>");
        }
        return new TokenMap(components[0], components[1], token);
    }

    /**
     * Get the double represented by the value of the provided TokenMap
     * If the value is not a double, throws an InvalidTokenException
     * @param tokenMap
     * @return double of the value in the TokenMap
     * @throws InvalidTokenException
     */
    public static double getDoubleTokenValue(TokenMap tokenMap) throws InvalidTokenException {
        try {
            return Double.parseDouble(tokenMap.getValue());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException(tokenMap.getToken(), String.format("Unable to convert string '%s' to double.", tokenMap.getValue(), e));
        }
    }
}
