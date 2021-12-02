package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

/**
 * {@inheritDoc}
 *
 * @param merlinGraphqlURI endpoint of the merlin graphql service that should be used to access all plan data
 */
public record GraphQLMerlinService(URI merlinGraphqlURI) implements MerlinService {

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPlanRevision(final String planId) throws NoSuchPlanException {
    //TODO: actually do this graphql query to merlin endpoint
    //TODO: deserialize the json result body (using some aerie json parsers?)
    final long planRev = 0;

    return planRev;
  }

  /**
   * {@inheritDoc}
   *
   * retrieves the metadata via a single atomic graphql query
   */
  @Override
  public PlanMetadata getPlanMetadata(final String planId) throws NoSuchPlanException {
    //TODO: actually do this graphql query to merlin endpoint
    //TODO: deserialize the json result body (using some aerie json parsers?)
    final long planRev = 0;
    final var startTime = Timestamp.fromString("2000-001T00:00:00");
    final var endTime = Timestamp.fromString("2000-001T01:00:00");
    final var modelId = "0";
    final var modelPath = Path.of("model.jar");
    final var modelName = "";
    final var modelVersion = "";
    final var modelConfiguration = Map.<String, SerializedValue>of();

    //TODO: unify scheduler/aerie time types to avoid conversions
    final var horizon = new PlanningHorizon(
        Time.fromString(startTime.toString()),
        Time.fromString(endTime.toString()));

    return new PlanMetadata(
        planId, planRev,
        horizon,
        modelId, modelPath, modelName, modelVersion,
        modelConfiguration);
  }

}
