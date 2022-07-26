package gov.nasa.jpl.aerie.merlin.protocol.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public final class SerializedValueDeserializer extends StdDeserializer<SerializedValue> {

  public SerializedValueDeserializer() {
    super((Class<?>) null);
  }
  public SerializedValueDeserializer(Class<SerializedValue> c) {
    super(c);
  }

  @Override
  public SerializedValue deserialize(JsonParser parser, DeserializationContext context)
  throws IOException
  {
    return processNode(parser.readValueAs(JsonNode.class));
  }

  private SerializedValue processNode(final JsonNode node) throws IOException {
    if (node.isArray()) {
      final var list = new ArrayList<SerializedValue>();
      final int size = node.size();
      for (int i = 0; i < size; i++) {
        list.add(processNode(node.get(i)));
      }
      return SerializedValue.of(list);
    } else if (node.isObject()) {
      final var map = new HashMap<String, SerializedValue>();
      for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
        final String name = it.next();
        map.put(name, processNode(node.get(name)));
      }
      return SerializedValue.of(map);
    } else if (node.isIntegralNumber()) {
      return SerializedValue.of(node.asLong());
    } else if (node.isTextual()) {
      return SerializedValue.of(node.asText());
    } else if (node.isBoolean()) {
      return SerializedValue.of(node.asBoolean());
    } else if (node.isDouble()) {
      return SerializedValue.of(node.asDouble());
    } else if (node.isNull()) {
      return SerializedValue.NULL;
    } else {
      throw new IOException("Serialized Value schema not recognized: " + node.toPrettyString());
    }
  }
}
