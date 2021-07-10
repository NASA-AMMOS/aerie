package gov.nasa.jpl.aerie.merlin.server;

import com.mongodb.client.MongoCollection;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoDeserializers;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoSerializers;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public final class ResultsProtocol {
  private ResultsProtocol() {}

  public /*sealed*/ interface State {
    /** Simulation in progress, but no results to share yet. */
    record Incomplete() implements State {}

    /** Simulation complete -- results now available. */
    record Success(SimulationResults results) implements State {}

    /** Simulation failed -- don't try to re-run without changing some of the inputs. */
    record Failed(String reason) implements State {}
  }

  public interface ReaderRole {
    State get();

    /** After calling cancel, `get` is no longer legal to invoke. */
    void cancel();
  }

  public interface WriterRole {
    boolean isCanceled();

    // If the writer aborts because it observes `isCanceled()`,
    //   it must still complete with `fail()`.
    //   Otherwise, the reader would not be able to reclaim unique ownership
    //   of the underlying resource in order to deallocate it.
    void succeedWith(SimulationResults results);
    void failWith(String reason);
  }

  public interface OwnerRole extends ReaderRole, WriterRole {
  }

  public static final class MongoCell implements OwnerRole {
    private final MongoCollection<Document> collection;
    public final ObjectId documentId;

    public MongoCell(final MongoCollection<Document> collection, final ObjectId documentId) {
      this.collection = Objects.requireNonNull(collection);
      this.documentId = Objects.requireNonNull(documentId);
    }

    public static MongoCell
    allocate(final MongoCollection<Document> collection, final String planId, final long planRevision) {
      return new MongoResultsCellRepository(collection).allocate(planId, planRevision);
    }

    public static Optional<ReaderRole>
    lookup(final MongoCollection<Document> collection, final String planId, final long planRevision) {
      return new MongoResultsCellRepository(collection).lookup(planId, planRevision);
    }

    public static void
    deallocate(final MongoCollection<Document> collection, final String planId, final long planRevision) {
      new MongoResultsCellRepository(collection).deallocate(planId, planRevision);
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

  public static final class InMemoryCell implements OwnerRole {
    private volatile boolean canceled = false;
    private volatile State state = new State.Incomplete();

    @Override
    public State get() {
      return this.state;
    }

    @Override
    public void cancel() {
      this.canceled = true;
    }

    @Override
    public boolean isCanceled() {
      return this.canceled;
    }

    @Override
    public void succeedWith(final SimulationResults results) {
      if (!(this.state instanceof State.Incomplete)) {
        throw new IllegalStateException("Cannot transition to success state from state %s".formatted(
            this.state.getClass().getCanonicalName()));
      }

      this.state = new State.Success(results);
    }

    @Override
    public void failWith(final String reason) {
      if (!(this.state instanceof State.Incomplete)) {
        throw new IllegalStateException("Cannot transition to failed state from state %s".formatted(
            this.state.getClass().getCanonicalName()));
      }

      this.state = new State.Failed(reason);
    }
  }
}
