package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public final class MongoResultsCellRepository implements ResultsCellRepository {
  private final MongoCollection<Document> collection;

  public MongoResultsCellRepository(final MongoDatabase database, final String resultsCellCollectionName) {
    this.collection = database.getCollection(resultsCellCollectionName);
  }

  @Override
  public MongoCell
  allocate(final String planId, final long planRevision)
  {
    final var document = new Document(Map.of(
        "planId", planId,
        "planRevision", planRevision,
        "state", MongoSerializers.simulationResultsState(new State.Incomplete()),
        "canceled", false));

    this.collection.insertOne(document);

    return new MongoCell(this.collection, document.getObjectId("_id"));
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole>
  lookup(final String planId, final long planRevision)
  {
    return Optional
        .ofNullable(this.collection
                        .find(and(eq("planId", planId), eq("planRevision", planRevision)))
                        .first())
        .map($ -> new MongoCell(this.collection, $.getObjectId("_id")));
  }

  @Override
  public void
  deallocate(final String planId, final long planRevision)
  {
    this.collection.deleteOne(and(eq("planId", planId), eq("planRevision", planRevision)));
  }

  public static final class MongoCell implements ResultsProtocol.OwnerRole {
    private final MongoCollection<Document> collection;
    public final ObjectId documentId;

    public MongoCell(final MongoCollection<Document> collection, final ObjectId documentId) {
      this.collection = Objects.requireNonNull(collection);
      this.documentId = Objects.requireNonNull(documentId);
    }

    @Override
    public State get() {
      final var resultsDocument = Optional
          .ofNullable(this.collection
              .find(eq("_id", this.documentId))
              .first())
          .orElseThrow(MissingDocumentException::new)
          .get("state", Document.class);

      return MongoDeserializers.simulationResultsState(resultsDocument);
    }

    @Override
    public void cancel() {
      this.collection.updateOne(eq("_id", this.documentId), set("canceled", true));
    }

    @Override
    public boolean isCanceled() {
      return Optional
          .ofNullable(this.collection
              .find(eq("_id", this.documentId))
              .first())
          .orElseThrow(MissingDocumentException::new)
          .getBoolean("canceled");
    }

    @Override
    public void succeedWith(final SimulationResults results) {
      this.collection.updateOne(
          and(eq("_id", this.documentId)),
          set("state", MongoSerializers.simulationResultsState(new State.Success(results))));
    }

    @Override
    public void failWith(final String reason) {
      this.collection.updateOne(
          and(eq("_id", this.documentId)),
          set("state", MongoSerializers.simulationResultsState(new State.Failed(reason))));
    }

    public static class MissingDocumentException extends RuntimeException {}
  }
}
