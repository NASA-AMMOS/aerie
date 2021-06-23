package gov.nasa.jpl.aerie.merlin.server.services;

import com.mongodb.client.MongoCollection;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import org.bson.Document;

import java.util.Objects;

public final class ThreadedMongoSimulationService implements SimulationService {
  private final MongoCollection<Document> resultsCollection;
  private final SimulationAgent agent;

  public ThreadedMongoSimulationService(
      final MongoCollection<Document> resultsCollection,
      final SimulationAgent agent)
  {
    this.resultsCollection = Objects.requireNonNull(resultsCollection);
    this.agent = Objects.requireNonNull(agent);
  }

  @Override
  public ResultsProtocol.State getSimulationResults(final String planId, final long planRevision) {
    final var cell$ = ResultsProtocol.MongoCell.lookup(this.resultsCollection, planId, planRevision);
    if (cell$.isPresent()) {
      return cell$.get().get();
    } else {
      // Allocate a fresh cell.
      final var cell = ResultsProtocol.MongoCell.allocate(this.resultsCollection, planId, planRevision);

      // Split the cell into its two concurrent roles, and delegate the writer role to another process.
      final ResultsProtocol.ReaderRole reader;
      try {
        final ResultsProtocol.WriterRole writer = cell;
        reader = cell;

        this.agent.simulate(planId, planRevision, writer);
      } catch (final InterruptedException ex) {
        // If we couldn't delegate, clean up the cell and return an Incomplete.
        ResultsProtocol.MongoCell.deallocate(this.resultsCollection, planId, planRevision);
        return new ResultsProtocol.State.Incomplete();
      }

      // Return the current value of the reader; if it's incomplete, the caller can check it again later.
      return reader.get();
    }
  }
}
