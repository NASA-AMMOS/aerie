package gov.nasa.jpl.ammos.mpsa.aerie.simulation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.models.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils.InvalidEntityFailure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ResponseSerializers {
  private ResponseSerializers() {}

  private static JsonValue serializeBreadcrumb(final Breadcrumb breadcrumb) {
    return breadcrumb.match(new Breadcrumb.SafeVisitor<>() {
      @Override
      public JsonValue onListIndex(final int index) {
        return Json.createValue(index);
      }

      @Override
      public JsonValue onMapIndex(final String index) {
        return Json.createValue(index);
      }
    });
  }

  public static JsonValue serializeBreadcrumbMessage(final List<Breadcrumb> breadcrumbs, final String message) {
    final var breadcrumbsJson = Json.createArrayBuilder();
    for (final var breadcrumb : breadcrumbs) {
      breadcrumbsJson.add(serializeBreadcrumb(breadcrumb));
    }

    return Json.createObjectBuilder()
        .add("breadcrumbs", breadcrumbsJson)
        .add("message", message)
        .build();
  }

  public static JsonValue serializeBreadcrumbMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    final var builder = Json.createArrayBuilder();
    for (final var entry : messages) {
      builder.add(serializeBreadcrumbMessage(entry.getKey(), entry.getValue()));
    }

    return builder.build();
  }

  static public JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    final var visitor = new FlattenFailuresVisitor();
    for (final var failure : ex.failures) failure.match(visitor);

    return serializeBreadcrumbMessages(visitor.failures);
  }

  static public JsonValue serializeTimestamp(final Time timestamp) {
    return Json.createValue(timestamp.toTAI());
  }

  static public JsonValue serializeTimestamps(final List<Time> elements) {
    final var builder = Json.createArrayBuilder();
    for (final var element : elements) {
      builder.add(serializeTimestamp(element));
    }
    return builder.build();
  }

  static public JsonValue serializeParameter(final SerializedParameter parameter) {
    return parameter.match(new ParameterSerializationVisitor());
  }

  static public JsonValue serializeTimeline(final List<SerializedParameter> elements) {
    final var builder = Json.createArrayBuilder();
    for (final var element : elements) {
      builder.add(serializeParameter(element));
    }
    return builder.build();
  }

  static public JsonValue serializeSimulationResults(final SimulationResults results) {
    final var builder = Json.createObjectBuilder();
    builder.add("timestamps", serializeTimestamps(results.timestamps));
    for (int i = 0; i < results.timelines.size(); ++i) {
      builder.add("state-" + i, serializeTimeline(results.timelines.get(i)));
    }
    return builder.build();
  }

  static private class FlattenFailuresVisitor implements InvalidEntityFailure.Visitor<Object, RuntimeException> {
    private final List<Breadcrumb> trail = new ArrayList<>();

    public final List<Pair<List<Breadcrumb>, String>> failures = new ArrayList<>();

    @Override
    public Object onScope(final Breadcrumb breadcrumb, final List<InvalidEntityFailure> scope) {
      trail.add(breadcrumb);
      for (final var failure : scope) failure.match(this);
      trail.remove(trail.size() - 1);
      return null;
    }

    @Override
    public Object onMessage(final String message) {
      failures.add(Pair.of(List.copyOf(trail), message));
      return null;
    }
  }

  static private class ParameterSerializationVisitor implements SerializedParameter.Visitor<JsonValue> {
    @Override
    public JsonValue onNull() {
      return JsonValue.NULL;
    }

    @Override
    public JsonValue onReal(final double value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onInt(final long value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onBoolean(final boolean value) {
      return (value) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    @Override
    public JsonValue onString(final String value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onMap(final Map<String, SerializedParameter> fields) {
      final var builder = Json.createObjectBuilder();
      for (final var field : fields.entrySet()) {
        builder.add(field.getKey(), field.getValue().match(this));
      }
      return builder.build();
    }

    @Override
    public JsonValue onList(final List<SerializedParameter> elements) {
      final var builder = Json.createArrayBuilder();
      for (final var element : elements) {
        builder.add(element.match(this));
      }
      return builder.build();
    }
  }
}
