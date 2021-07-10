package gov.nasa.jpl.aerie.merlin.server;

import com.mongodb.client.MongoCollection;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoSerializers;
import org.bson.Document;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoResultsCellRepository implements ResultsCellRepository {
  private final MongoCollection<Document> collection;

  public MongoResultsCellRepository(final MongoCollection<Document> collection) {
    this.collection = Objects.requireNonNull(collection);
  }

  @Override
  public ResultsProtocol.MongoCell
  allocate(final String planId, final long planRevision)
  {
    final var document = new Document(Map.of(
        "planId", planId,
        "planRevision", planRevision,
        "state", MongoSerializers.simulationResultsState(new ResultsProtocol.State.Incomplete()),
        "canceled", false));

    collection.insertOne(document);

    return new ResultsProtocol.MongoCell(collection, document.getObjectId("_id"));
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole>
  lookup(final String planId, final long planRevision)
  {
    return Optional
        .ofNullable(collection
                        .find(and(eq("planId", planId), eq("planRevision", planRevision)))
                        .first())
        .map($ -> new ResultsProtocol.MongoCell(collection, $.getObjectId("_id")));
  }

  @Override
  public void
  deallocate(final String planId, final long planRevision)
  {
    collection.deleteOne(and(eq("planId", planId), eq("planRevision", planRevision)));
  }
}
