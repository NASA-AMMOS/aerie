package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ParallelInserter {
  private final List<Consumer<Connection>> actions = new ArrayList<>();
  private final Map<String, List<Consumer<PreparedStatement>>> inserts = new HashMap<>();
  private boolean canDeclareActions = true;
  private boolean canDeclareInserts = true;

  void declareAction(final Consumer<Connection> action) {
    if (!canDeclareActions) throw new IllegalStateException("Cannot declare action during execution");
    actions.add(action);
  }
  void declareInsert(final String sql, final Consumer<PreparedStatement> statementPopulator) {
    if (!canDeclareInserts) throw new IllegalStateException("Cannot declare inserts during inserts");
    inserts
        .computeIfAbsent(sql, $ -> new ArrayList<>())
        .add(statementPopulator);
  }
  void execute(final DataSource dataSource) throws SQLException {
    this.canDeclareActions = false;
    try (final var transaction = new TransactionContext(dataSource.getConnection())) {
      for (final var action : actions) {
        action.accept(transaction.connection);
      }
      transaction.commit();
    }

    // NOTE: It is legal for actions to declare inserts (but not other actions).
    this.canDeclareInserts = false;

    var total = 0;
    for (final var entry : inserts.entrySet()) {
      total += entry.getValue().size();
    }

    if (total < 10000) {
      // Not worth parallelizing
      runInserts(dataSource.getConnection(), inserts);
      return;
    }

    final var numThreads = 3;

    final var insertsPerThread = 1 + (total / numThreads);
    // split into batches

    final var batches = new ArrayList<Map<String, List<Consumer<PreparedStatement>>>>();

    var currentBatch = new HashMap<String, List<Consumer<PreparedStatement>>>();
    var currentBatchSize = 0;
    for (final var entry : inserts.entrySet()) {
      for (final var statementPopulator : entry.getValue()) {
        if (currentBatchSize > insertsPerThread) {
          batches.add(currentBatch);
          currentBatch = new HashMap<>();
          currentBatchSize = 0;
        }
        currentBatch
            .computeIfAbsent(entry.getKey(), $ -> new ArrayList<>())
            .add(statementPopulator);
        currentBatchSize++;
      }
    }
    if (currentBatchSize > 0) {
      batches.add(currentBatch);
    }

    System.out.println("Spinning up " + numThreads + " threads to run " + batches.size() + " batches of size " + insertsPerThread);

    try (final var executor = Executors.newFixedThreadPool(numThreads)) {
      for (final var batch : batches) {
        executor
            .execute(() -> {
              try {
                runInserts(dataSource.getConnection(), batch);
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            });
      }
    }
  }

  private static void runInserts(
      final Connection connection,
      final Map<String, List<Consumer<PreparedStatement>>> inserts) throws SQLException {
    try (final var transaction = new TransactionContext(connection)) {
      for (final var entry : inserts.entrySet()) {
        try (final var statement = transaction.connection.prepareStatement(entry.getKey())) {
          for (final var statementPopulator : entry.getValue()) {
            statementPopulator.accept(statement);
            statement.addBatch();
          }
          statement.executeBatch();
        }
      }
      transaction.commit();
    }
  }

  interface Consumer<T> {
    void accept(T t) throws SQLException;
  }
}
