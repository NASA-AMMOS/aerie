package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MongoDeserializers {
  private MongoDeserializers() {}

  public static <Element> Map<String, Element> map(
      final Document document,
      final Function<Document, Element> elementDeserializer
  ) {
    final var elements = new HashMap<String, Element>();
    for (final var entry : document.entrySet()) {
      elements.put(entry.getKey(), elementDeserializer.apply((Document) entry.getValue()));
    }
    return elements;
  }

  public static Instant instant(final String timestamp) {
    return Timestamp
        .fromString(timestamp)
        .toInstant();
  }

  public static Duration duration(final long epsilons) {
    return Duration.EPSILON.times(epsilons);
  }

  public static ValueSchema valueSchema(final Document document) {
    switch (document.getString("type")) {
      case "real":
        return ValueSchema.REAL;
      case "int":
        return ValueSchema.INT;
      case "boolean":
        return ValueSchema.BOOLEAN;
      case "string":
        return ValueSchema.STRING;
      case "duration":
        return ValueSchema.DURATION;
      case "series":
        return ValueSchema.ofSeries(valueSchema(document.get("items", Document.class)));
      case "struct":
        final var fieldsDocument = (Document) document.get("items");

        final var fields = new HashMap<String, ValueSchema>();
        for (final var entry : fieldsDocument.entrySet()) {
          fields.put(entry.getKey(), valueSchema((Document) entry.getValue()));
        }

        return ValueSchema.ofStruct(fields);
      case "variant":
        // SAFETY: It's not safe, really, but the Document abstraction is so leaky I think we have to do this.
        @SuppressWarnings("unchecked")
        final var variantsDocuments = (List<Document>) document.get("variants");

        final var variants = new ArrayList<ValueSchema.Variant>();
        for (final var variantDocument : variantsDocuments) {
          variants.add(new ValueSchema.Variant(
              variantDocument.getString("key"),
              variantDocument.getString("label")));
        }

        return ValueSchema.ofVariant(variants);
      default:
        throw new Error("unexpected bad data in database");
    }
  }

  public static SerializedValue serializedValue(final Document document) {
    switch (document.getString("type")) {
      case "null":
        return SerializedValue.NULL;
      case "string":
        return SerializedValue.of(document.getString("value"));
      case "int":
        return SerializedValue.of(document.getLong("value"));
      case "double":
        return SerializedValue.of(document.getDouble("value"));
      case "boolean":
        return SerializedValue.of(document.getBoolean("value"));
      case "list":
        final List<Document> itemDocuments = document.getList("value", Document.class);

        final List<SerializedValue> items = new ArrayList<>();
        for (final var itemDocument : itemDocuments) {
          items.add(serializedValue(itemDocument));
        }

        return SerializedValue.of(items);
      case "map":
        final Document fieldsDocument = document.get("value", Document.class);

        final Map<String, SerializedValue> fields = new HashMap<>();
        for (final var entry : fieldsDocument.entrySet()) {
          fields.put(entry.getKey(), serializedValue((Document)entry.getValue()));
        }

        return SerializedValue.of(fields);
      default:
        throw new Error("unexpected bad data in database");
    }
  }

  public static RealDynamics realDynamics(final Document document) {
    return RealDynamics.linear(document.getDouble("initial"), document.getDouble("rate"));
  }

  public static <Dynamics> Pair<Duration, Dynamics> profileSegment(
      final Document document,
      final Function<Document, Dynamics> dynamicsDeserializer
  ) {
    return Pair.of(
        Duration.EPSILON.times(document.getLong("at")),
        dynamicsDeserializer.apply(document.get("value", Document.class)));
  }

  public static <Dynamics> List<Pair<Duration, Dynamics>> profile(
      final List<Document> segmentDocuments,
      final Function<Document, Dynamics> dynamicsDeserializer
  ) {
    final var segments = new ArrayList<Pair<Duration, Dynamics>>(segmentDocuments.size());
    for (final var segmentDocument : segmentDocuments) {
      segments.add(profileSegment(segmentDocument, dynamicsDeserializer));
    }
    return segments;
  }

  public static Pair<ValueSchema, List<Pair<Duration, SerializedValue>>> discreteProfile(
      final Document profileDocument
  ) {
    // SAFETY: It's not safe, really, but the Document abstraction is so leaky I think we have to do this.
    @SuppressWarnings("unchecked")
    final var segmentDocuments = (List<Document>) profileDocument.get("segments");

    return Pair.of(
        MongoDeserializers.valueSchema(profileDocument.get("schema", Document.class)),
        MongoDeserializers.profile(segmentDocuments, MongoDeserializers::serializedValue));
  }

  public static List<Pair<Duration, RealDynamics>> realProfile(final Document profileDocument) {
    // SAFETY: It's not safe, really, but the Document abstraction is so leaky I think we have to do this.
    @SuppressWarnings("unchecked")
    final var segmentDocuments = (List<Document>) profileDocument.get("segments");

    return MongoDeserializers.profile(segmentDocuments, MongoDeserializers::realDynamics);
  }

  public static SimulatedActivity simulatedActivity(final Document activityDocument) {
    // SAFETY: It's not safe, really, but the Document abstraction is so leaky I think we have to do this.
    @SuppressWarnings("unchecked")
    final var childIds = (List<String>) activityDocument.get("childIds");

    return new SimulatedActivity(
          activityDocument.getString("type"),
          MongoDeserializers.map(
              activityDocument.get("parameters", Document.class),
              MongoDeserializers::serializedValue),
          MongoDeserializers.instant(activityDocument.getString("start")),
          MongoDeserializers.duration(activityDocument.getLong("duration")),
          activityDocument.getString("parentId"),
          childIds);
  }

  public static SerializedActivity serializedActivity(final Document activityDocument) {
    return new SerializedActivity(
          activityDocument.getString("type"),
          MongoDeserializers.map(
              activityDocument.get("parameters", Document.class),
              MongoDeserializers::serializedValue));
  }

  public static SimulationResults simulationResults(final Document resultsDocument) {
    return new SimulationResults(
        MongoDeserializers.map(
            resultsDocument.get("realProfiles", Document.class),
            MongoDeserializers::realProfile),
        MongoDeserializers.map(
            resultsDocument.get("discreteProfiles", Document.class),
            MongoDeserializers::discreteProfile),
        MongoDeserializers.map(
            resultsDocument.get("finishedActivities", Document.class),
            MongoDeserializers::simulatedActivity),
        MongoDeserializers.map(
            resultsDocument.get("unfinishedActivities", Document.class),
            MongoDeserializers::serializedActivity),
        MongoDeserializers.instant(
            resultsDocument.getString("startTime")));
  }

  public static ResultsProtocol.State simulationResultsState(final Document stateDocument) {
    final var type = stateDocument.getString("type");

    return switch (type) {
      case "incomplete" ->
          new ResultsProtocol.State.Incomplete();
      case "failed" ->
          new ResultsProtocol.State.Failed(stateDocument.getString("reason"));
      case "success" ->
          new ResultsProtocol.State.Success(simulationResults(stateDocument.get("results", Document.class)));
      default ->
          throw new Error("Key `%s` has unexpected value `%s`".formatted("type", type));
    };
  }
}
