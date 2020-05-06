package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonUtilities {
    public static Map<String, String> parseStringStringMap(InputStream jsonStream) throws IOException {
        JsonParser parser = new JsonFactory().createParser(jsonStream);
        return new ObjectMapper().readValue(parser, new TypeReference<Map<String, String>>() {});
    }
}
