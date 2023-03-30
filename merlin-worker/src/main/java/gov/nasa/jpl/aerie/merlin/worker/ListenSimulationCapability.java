package gov.nasa.jpl.aerie.merlin.worker;

import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.DatabaseException;
import gov.nasa.jpl.aerie.merlin.worker.postgres.ListenSimulationStatusAction;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresSimulationNotificationPayload;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import static gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresNotificationJsonParsers.postgresSimulationNotificationP;

public class ListenSimulationCapability {
  private static final Logger logger = LoggerFactory.getLogger(ListenSimulationCapability.class);

  private final DataSource dataSource;
  private final BlockingQueue<PostgresSimulationNotificationPayload> notificationQueue;

  public ListenSimulationCapability(
      final DataSource dataSource,
      final BlockingQueue<PostgresSimulationNotificationPayload> notificationQueue) {
    this.dataSource = dataSource;
    this.notificationQueue = notificationQueue;
  }

  public void registerListener() {
    new Thread(() -> {
      try (final var connection = this.dataSource.getConnection()) {
        try (final var listenSimulationStatusAction = new ListenSimulationStatusAction(connection)) {
          listenSimulationStatusAction.apply();
        } catch (final SQLException ex) {
          throw new DatabaseException("Failed to register as LISTEN to postgres database.", ex);
        }

        while (true) {
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
                final var notificationPayload = postgresSimulationNotificationP
                    .parse(jsonValue)
                    .getSuccessOrThrow();
                try {
                  this.notificationQueue.put(notificationPayload);
                } catch (InterruptedException e) {
                  // We do not expect this thread to be interrupted. If it is, exit gracefully:
                  return;
                }
              }
            }
          }
        }
      } catch (SQLException e) {
        throw new DatabaseException("Listener encountered exception", e);
      }
    }).start();
  }
}
