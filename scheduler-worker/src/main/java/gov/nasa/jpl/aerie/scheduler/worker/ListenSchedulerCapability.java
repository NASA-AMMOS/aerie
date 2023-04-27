package gov.nasa.jpl.aerie.scheduler.worker;

import static gov.nasa.jpl.aerie.scheduler.worker.postgres.PostgresNotificationJsonParsers.postgresSchedulingRequestNotificationP;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.DatabaseException;
import gov.nasa.jpl.aerie.scheduler.worker.postgres.ListenSchedulingRequestStatusAction;
import gov.nasa.jpl.aerie.scheduler.worker.postgres.PostgresSchedulingRequestNotificationPayload;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import javax.json.Json;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenSchedulerCapability {
  private static final Logger logger = LoggerFactory.getLogger(ListenSchedulerCapability.class);

  private final DataSource dataSource;
  private final BlockingQueue<PostgresSchedulingRequestNotificationPayload> notificationQueue;

  public ListenSchedulerCapability(
      final DataSource dataSource,
      final BlockingQueue<PostgresSchedulingRequestNotificationPayload> notificationQueue) {
    this.dataSource = dataSource;
    this.notificationQueue = notificationQueue;
  }

  public void registerListener() throws SQLException {
    PGConnection connection = this.dataSource.getConnection().unwrap(PGConnection.class);
    connection.addNotificationListener(new SchedulingRequestNotificationListener());

    try (final var listenSchedulingRequestStatusAction =
        new ListenSchedulingRequestStatusAction(connection)) {
      listenSchedulingRequestStatusAction.apply();
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to register as LISTEN to postgres database.", ex);
    }
  }

  private final class SchedulingRequestNotificationListener implements PGNotificationListener {

    @Override
    public void notification(final int processId, final String channelName, final String payload) {
      logger.info("Received PSQL Notification: {}, {}, {}", processId, channelName, payload);
      final var jsonValue = Json.createReader(new StringReader(payload)).readValue();
      final var notificationPayload =
          postgresSchedulingRequestNotificationP.parse(jsonValue).getSuccessOrThrow();
      try {
        notificationQueue.put(notificationPayload);
      } catch (InterruptedException ex) {
        // We are unable to handle this exception here so the thread interrupt flag is set to true
        // so that code higher up the call stack can see that an interrupt was issued.
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void closed() {
      PGNotificationListener.super.closed();
    }
  }
}
