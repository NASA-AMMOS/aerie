package gov.nasa.jpl.aerie.services.cli.matchers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Used to match JSON strings based on their content
 * as opposed to matching each character in a string
 */
public class JsonMatcher {

    private JsonParser parser;
    private JsonElement json;

    public JsonMatcher(String body) {
        parser = new JsonParser();
        json = parser.parse(body);
    }

    public boolean matches(Object o) {
        return o instanceof String && json.equals(parser.parse((String)o));
    }
}
