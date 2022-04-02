package gov.nasa.jpl.aerie.merlin.worker;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.DatabaseException;
import gov.nasa.jpl.aerie.merlin.worker.postgres.ListenSimulationStatusAction;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresSimulationNotificationPayload;
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

  public void registerListener() throws SQLException {
    PGConnection connection = this.dataSource.getConnection().unwrap(PGConnection.class);
    connection.addNotificationListener(new SimulationNotificationListener());

    try (final var listenSimulationStatusAction = new ListenSimulationStatusAction(connection)) {
       listenSimulationStatusAction.apply();
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to register as LISTEN to postgres database.", ex);
    }
  }

  private class SimulationNotificationListener implements PGNotificationListener {

    @Override
    public void notification(int processId, String channelName, String payload){
      logger.info("Received PSQL Notification: {}, {}, {}", processId, channelName, payload);
      final var jsonValue = Json.createReader(new StringReader(payload)).readValue();
      final var notificationPayload = postgresSimulationNotificationP
          .parse(jsonValue)
          .getSuccessOrThrow();
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
