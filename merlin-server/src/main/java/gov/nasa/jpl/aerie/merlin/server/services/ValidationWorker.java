package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService.NoSuchMissionModelException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;
import java.util.List;

public record ValidationWorker(LocalMissionModelService missionModelService, int pollingPeriod) {

  private static final Logger logger = LoggerFactory.getLogger(ValidationWorker.class);

  public void workerLoop() {
    logger.info("validation worker starting...");
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(pollingPeriod);

        final var validationRequests = missionModelService.getUnvalidatedDirectives();
        if (!validationRequests.isEmpty()) {
          logger.debug(
              "queried {} directives that need validations, across {} models",
              validationRequests.size(),
              validationRequests.keySet().size());
        }

        for (final var entry : validationRequests.entrySet()) {
          final var beginTime = System.nanoTime();
          final var modelId = entry.getKey();
          logger.debug("processing batch for mission model: {}", modelId.toString());
          final var unvalidatedDirectives = entry.getValue();
          final var responses = missionModelService.validateActivityArgumentsBulk(modelId, unvalidatedDirectives);

          // zip lists together
          final List<Pair<ActivityDirectiveForValidation, MissionModelService.BulkArgumentValidationResponse>>
              zippedList = IntStream
                .range(0, Math.min(unvalidatedDirectives.size(), responses.size()))
                .mapToObj(i -> Pair.of(
                    unvalidatedDirectives.get(i),
                    responses.get(i)))
                .toList();

          // write validations out to DB
          missionModelService.updateDirectiveValidations(zippedList);
          final var endTime = System.nanoTime();
          final var duration = (endTime - beginTime) / 1_000_000.0;
          logger.debug("processed model batch of size {} in {} ms", unvalidatedDirectives.size(), duration);
        }

      } catch (NoSuchMissionModelException ex) {
        logger.error("Validation request failed due to no such mission model: {}", ex.toString());
      } catch (InterruptedException ex) {
        throw new RuntimeException("Failed to sleep in validation thread", ex);
      }
    }
  }
}
