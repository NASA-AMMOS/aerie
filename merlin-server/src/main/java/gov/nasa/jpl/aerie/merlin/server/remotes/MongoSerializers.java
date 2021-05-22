package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MongoSerializers {
  private MongoSerializers() {}

  public static <Element> List<Document> list(final List<Element> elements, final Function<Element, Document> elementSerializer) {
    final var documents = new ArrayList<Document>(elements.size());
    for (final var element : elements) {
      documents.add(elementSerializer.apply(element));
    }
    return documents;
  }

  public static <Element> Document map(final Map<String, Element> fields, final Function<Element, Document> elementSerializer) {
    final var document = new Document();
    for (final var entry : fields.entrySet()) {
      document.put(entry.getKey(), elementSerializer.apply(entry.getValue()));
    }
    return document;
  }

  public static String instant(final Instant instant) {
    return Timestamp.format.format(instant.atZone(ZoneOffset.UTC));
  }

  public static long duration(final Duration duration) {
    return duration.in(Duration.EPSILON);
  }

  public static Document valueSchema(final ValueSchema schema) {
    if (schema == null) return null;

    return schema.match(new ValueSchema.Visitor<>() {
      @Override
      public Document onReal() {
        return new Document(Map.of("type", "real"));
      }

      @Override
      public Document onInt() {
        return new Document(Map.of("type", "int"));
      }

      @Override
      public Document onBoolean() {
        return new Document(Map.of("type", "boolean"));
      }

      @Override
      public Document onString() {
        return new Document(Map.of("type", "string"));
      }

      @Override
      public Document onDuration() {
        return new Document(Map.of("type", "duration"));
      }

      @Override
      public Document onSeries(final ValueSchema itemSchema) {
        return new Document(Map.of(
            "type", "series",
            "items", itemSchema.match(this)));
      }

      @Override
      public Document onStruct(final Map<String, ValueSchema> parameterSchemas) {
        return new Document(Map.of(
            "type", "struct",
            "items", map(parameterSchemas, $ -> $.match(this))));
      }

      @Override
      public Document onVariant(final List<ValueSchema.Variant> variants) {
        return new Document(Map.of(
            "type", "variant",
            "variants", list(variants, v -> new Document(Map.of(
                "key", v.key,
                "label", v.label)))));
      }
    });
  }

  public static Document serializedValue(final SerializedValue parameter) {
    return parameter.match(new SerializedValue.Visitor<>() {
      @Override
      public Document onNull() {
        final Document document = new Document();
        document.put("type", "null");
        return document;
      }

      @Override
      public Document onReal(double value) {
        final Document document = new Document();
        document.put("type", "double");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onInt(long value) {
        final Document document = new Document();
        document.put("type", "int");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onBoolean(boolean value) {
        final Document document = new Document();
        document.put("type", "boolean");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onString(String value) {
        final Document document = new Document();
        document.put("type", "string");
        document.put("value", value);
        return document;
      }

      @Override
      public Document onList(List<SerializedValue> items) {
        final List<Document> itemDocuments = new ArrayList<>();
        for (final var item : items) {
          itemDocuments.add(item.match(this));
        }

        final Document document = new Document();
        document.put("type", "list");
        document.put("value", itemDocuments);
        return document;
      }

      @Override
      public Document onMap(Map<String, SerializedValue> fields) {
        final Document fieldsDocument = new Document();
        for (final var field : fields.entrySet()) {
          fieldsDocument.put(field.getKey(), field.getValue().match(this));
        }

        final Document document = new Document();
        document.put("type", "map");
        document.put("value", fieldsDocument);
        return document;
      }
    });
  }

  public static Document realDynamics(final RealDynamics dynamics) {
    return new Document(Map.of(
        "initial", dynamics.initial,
        "rate", dynamics.rate));
  }

  public static <Dynamics> Document profileSegment(
      final Pair<Duration, Dynamics> segment,
      final Function<Dynamics, Document> dynamicsSerializer
  ) {
    return new Document(Map.of(
        "at", duration(segment.getLeft()),
        "value", dynamicsSerializer.apply(segment.getRight())));
  }

  public static <Dynamics> List<Document> profile(
      final List<Pair<Duration, Dynamics>> segments,
      final Function<Dynamics, Document> dynamicsSerializer
  ) {
    final var segmentDocuments = new ArrayList<Document>(segments.size());
    for (final var segment : segments) {
      segmentDocuments.add(profileSegment(segment, dynamicsSerializer));
    }
    return segmentDocuments;
  }

  public static Document realProfile(final List<Pair<Duration, RealDynamics>> segments) {
    return new Document(Map.of(
        "segments", profile(segments, MongoSerializers::realDynamics)));
  }

  public static Document discreteProfile(final Pair<ValueSchema, List<Pair<Duration, SerializedValue>>> profile) {
    return new Document(Map.of(
        "schema", valueSchema(profile.getLeft()),
        "segments", profile(profile.getRight(), MongoSerializers::serializedValue)));
  }

  public static Document simulatedActivity(final SimulatedActivity activity) {
    final var activityDocument = new Document(Map.of(
        "type", activity.type,
        "parameters", map(activity.parameters, MongoSerializers::serializedValue),
        "childIds", activity.childIds,
        "start", instant(activity.start),
        "duration", duration(activity.duration)));

    // `activity.parentId` is nullable, so it can't be passed to `Map.of()`.
    activityDocument.put("parentId", activity.parentId);

    return activityDocument;
  }

  public static Document serializedActivity(final SerializedActivity activity) {
    return new Document(Map.of(
        "type", activity.getTypeName(),
        "parameters", map(activity.getParameters(), MongoSerializers::serializedValue)));
  }

  public static Document simulationResults(final SimulationResults results) {
    return new Document(Map.of(
        "startTime", instant(results.startTime),
        "realProfiles", map(results.realProfiles, MongoSerializers::realProfile),
        "discreteProfiles", map(results.discreteProfiles, MongoSerializers::discreteProfile),
        "finishedActivities", map(results.simulatedActivities, MongoSerializers::simulatedActivity),
        "unfinishedActivities", map(results.unfinishedActivities, MongoSerializers::serializedActivity)));
  }
}
