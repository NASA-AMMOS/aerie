package gov.nasa.jpl.aerie.merlin.worker.capabilities;

import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRevisionData;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresSimulationNotificationPayload;
import gov.nasa.jpl.aerie.merlin.worker.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public record HandleSimulationCapability(
    HikariDataSource hikariDataSource,
    LinkedBlockingQueue<PostgresSimulationNotificationPayload> simulationNotificationQueue,

    Stores stores,
    SynchronousSimulationAgent simulationAgent) {

  private static final Logger logger = LoggerFactory.getLogger(HandleSimulationCapability.class);

  public Thread registerHandler() {
    return new Thread(() -> {
      logger.info("simulation handler thread starting...");
      while (!Thread.currentThread().isInterrupted()) {
        final PostgresSimulationNotificationPayload notification;
        try {
          notification = simulationNotificationQueue.poll(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        if (notification == null) continue;

        final var planId = new PlanId(notification.planId());
        final var datasetId = notification.datasetId();

        final Optional<ResultsProtocol.OwnerRole> owner = stores.results().claim(planId, datasetId);
        if (owner.isEmpty()) continue;

        final var revisionData = new PostgresPlanRevisionData(
            notification.modelRevision(),
            notification.planRevision(),
            notification.simulationRevision(),
            notification.simulationTemplateRevision());
        final ResultsProtocol.WriterRole writer = owner.get();
        try {
          logger.info("starting sim with plan id: {}", planId);
          simulationAgent.simulate(planId, revisionData, writer);
          logger.info("finished sim with plan id: {}", planId);
        } catch (final Throwable ex) {
          ex.printStackTrace(System.err);
          writer.failWith(b -> b
              .type("UNEXPECTED_SIMULATION_EXCEPTION")
              .message("Something went wrong while simulating")
              .trace(ex));
        }
      }
    });
  }
}
