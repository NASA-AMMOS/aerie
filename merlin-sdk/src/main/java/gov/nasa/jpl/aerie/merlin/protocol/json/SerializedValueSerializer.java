package gov.nasa.jpl.aerie.merlin.protocol.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class SerializedValueSerializer extends StdSerializer<SerializedValue> {

  public SerializedValueSerializer() {
    this(null);
  }
  public SerializedValueSerializer(Class<SerializedValue> c) {
    super(c);
  }

  @Override
  public void serialize(final SerializedValue value, final JsonGenerator gen, final SerializerProvider provider)
  throws IOException
  {
    final var error = value.match(new SerializedValue.Visitor<IOException>() {
      @Override
      public IOException onNull() {
        try {
          gen.writeNull();
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onReal(final double value) {
        try {
          gen.writeNumber(value);
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onInt(final long value) {
        try {
          gen.writeNumber(value);
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onBoolean(final boolean value) {
        try {
          gen.writeBoolean(value);
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onString(final String value) {
        try {
          gen.writeString(value);
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onMap(final Map<String, SerializedValue> value) {
        try {
          gen.writeStartObject();
          for (String key: value.keySet()) {
            gen.writeFieldName(key);
            serialize(value.get(key), gen, provider);
          }
          gen.writeEndObject();
        } catch (IOException e) {
          return e;
        }
        return null;
      }

      @Override
      public IOException onList(final List<SerializedValue> value) {
        try {
          gen.writeStartArray();
          for (SerializedValue element: value) {
            serialize(element, gen, provider);
          }
          gen.writeEndArray();
        } catch (IOException e) {
          return e;
        }
        return null;
      }
    });

    if (error != null) {
      throw error;
    }
  }
}
