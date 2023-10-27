package gov.nasa.jpl.aerie.merlin.worker.capabilities;

import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.DatabaseException;
import gov.nasa.jpl.aerie.merlin.worker.postgres.ListenValidationQueueAction;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresValidationNotificationPayload;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import static gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresNotificationJsonParsers.postgresValidationNotificationP;

public class ListenValidationCapability {
  private static final Logger logger = LoggerFactory.getLogger(ListenValidationCapability.class);

  private final DataSource dataSource;
  private final BlockingQueue<PostgresValidationNotificationPayload> notificationQueue;

  public ListenValidationCapability(
      final DataSource dataSource,
      final BlockingQueue<PostgresValidationNotificationPayload> notificationQueue) {
    this.dataSource = dataSource;
    this.notificationQueue = notificationQueue;
  }

  public Thread registerListener() {
      return new Thread(() -> {
      logger.info("validation listener thread starting...");
      try (final var connection = this.dataSource.getConnection()) {
        try (final var listenValidationQueueAction = new ListenValidationQueueAction(connection)) {
          listenValidationQueueAction.apply();
        } catch (final SQLException ex) {
          throw new DatabaseException("Failed to register as LISTEN to postgres database.", ex);
        }

        while (!Thread.currentThread().isInterrupted()) {
          final var pgConnection = connection.unwrap(PGConnection.class);
          final var notifications = pgConnection.getNotifications(10000);
          if (notifications != null) {
            for (final var notification : notifications) {
              final var processId = notification.getPID();
              final var channelName = notification.getName();
              final var payload = notification.getParameter();
              logger.info("Received PSQL Notification: {}, {}, {}", processId, channelName, payload);
              try (final var reader = Json.createReader(new StringReader(payload))) {
                final var jsonValue = reader.readValue();
                final var notificationPayload = postgresValidationNotificationP
                    .parse(jsonValue)
                    .getSuccessOrThrow();
                this.notificationQueue.put(notificationPayload);
              }
            }
          }
        }
        logger.info("Listener has received interrupted signal");
      } catch (SQLException | InterruptedException e) {
        throw new RuntimeException("Listener encountered exception", e);
      }
    });
  }
}
