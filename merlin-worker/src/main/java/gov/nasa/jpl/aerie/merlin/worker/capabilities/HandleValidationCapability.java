package gov.nasa.jpl.aerie.merlin.worker.capabilities;

import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresValidationNotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public record HandleValidationCapability(
    HikariDataSource hikariDataSource,
    LinkedBlockingQueue<PostgresValidationNotificationPayload> validationNotificationQueue,
    LocalMissionModelService missionModelService) {

  private static final Logger logger = LoggerFactory.getLogger(HandleValidationCapability.class);

  public void registerHandler() {
      logger.info("validation handler thread starting...");
      try (final var connection = this.hikariDataSource().getConnection()) {
        while (!Thread.currentThread().isInterrupted()) {
          // handle validation queue
          final var request = validationNotificationQueue.poll(1, TimeUnit.SECONDS);
          long startTime = System.nanoTime();
          if (request == null) continue;

          logger.info(
              """
              processing activity validation queue request:
                act id: {}, rev: {}, plan id: {}, model id: {}, type: {}, args: {}
              """,
              request.activityDirectiveId(),
              request.revision(),
              request.planId(),
              request.modelId(),
              request.typeName(),
              request.arguments()
          );

          final var serializedActivity = new SerializedActivity(request.typeName(), request.arguments());

          String jsonResults;
          try {
            final var notices = this.missionModelService().validateActivityArguments(
                String.valueOf(request.modelId()),
                serializedActivity);
            jsonResults = ResponseSerializers.serializeValidationNotices(notices).toString();
          } catch (InstantiationException e) {
            jsonResults = ResponseSerializers.serializeInstantiationException(e).toString();
          } catch (MissionModelService.NoSuchMissionModelException e) {
            jsonResults = ResponseSerializers.serializeNoSuchMissionModelException(e).toString();
          }

          long endTime = System.nanoTime();

          double duration = (endTime - startTime) / 1000000.0;
          logger.info("validation time: {} ms", duration);

          final var prepared = connection.prepareStatement("""
                                     update activity_directive_changelog
                                     set validation_results = ?::jsonb
                                     where (activity_directive_id, revision, plan_id) = (?, ?, ?)
                                     """
                                     );
          prepared.setString(1, jsonResults);
          prepared.setInt(2, request.activityDirectiveId());
          prepared.setInt(3, request.revision());
          prepared.setInt(4, request.planId());

          logger.info("""
                      writing validation results for act id: {}, rev: {}, plan: {}
                        results: {}
                      """, request.activityDirectiveId(), request.revision(), request.planId(), jsonResults);

          prepared.execute();
        }
      } catch (SQLException | InterruptedException e) {
        throw new RuntimeException(e);
      }
  }
}
