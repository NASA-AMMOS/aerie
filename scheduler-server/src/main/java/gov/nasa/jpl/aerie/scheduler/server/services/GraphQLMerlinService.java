package gov.nasa.jpl.aerie.scheduler.server.services;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.AerieController;
import gov.nasa.jpl.aerie.scheduler.MissionModelWrapper;
import gov.nasa.jpl.aerie.scheduler.Plan;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * {@inheritDoc}
 *
 * @param merlinGraphqlURI endpoint of the merlin graphql service that should be used to access all plan data
 */
public record GraphQLMerlinService(URI merlinGraphqlURI) implements MerlinService {

  /**
   * timeout for http graphql requests issued to aerie
   */
  private static final java.time.Duration httpTimeout = java.time.Duration.ofSeconds(60);

  /**
   * dispatch the given graphql request to aerie and collect the results
   *
   * absorbs any io errors and returns an empty response object in order to keep exception
   * signature of callers cleanly matching the MerlinService interface
   *
   * @param gqlStr the graphQL query or mutation to send to aerie
   * @return the json response returned by aerie, or an empty optional in case of io errors
   */
  private Optional<JsonObject> postRequest(String gqlStr) {
    try {
      final var reqBody = Json.createObjectBuilder().add("query", gqlStr).build();
      final var httpReq = HttpRequest
          .newBuilder().uri(merlinGraphqlURI).timeout(httpTimeout)
          .header("Content-Type", "application/json")
          .header("Accept-Encoding", "gzip, deflate, br")
          .header("Accept", "application/json")
          .header("Origin", merlinGraphqlURI.toString())
          .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
          .build();
      final var httpResp = HttpClient
          .newHttpClient().send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
      if (httpResp.statusCode() != 200) {
        //TODO: how severely to error out if aerie cannot be reached or has a 500 error or json is garbled etc etc?
        return Optional.empty();
      }
      final var respBody = Json.createReader(new GZIPInputStream(httpResp.body())).readObject();
      return Optional.of(respBody);
    } catch (IOException | InterruptedException | JsonException e) { // or also JsonParsingException
      //TODO: maybe retry if interrupted? but depends on semantics (eg don't duplicate mutation if not idempotent)
      return Optional.empty();
    }
  }

  //TODO: maybe use fancy aerie typed json parsers/serializers, ala BasicParsers.productP use in MerlinParsers
  //TODO: or upgrade to gson or similar modern library with registered object mappings

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPlanRevision(final String planId) throws NoSuchPlanException {
    final var request = "query getPlanRevision { plan_by_pk( id: %s ) { revision } }"
        .formatted(planId);
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      return response.getJsonObject("data").getJsonObject("plan_by_pk").getJsonNumber("revision").longValueExact();
    } catch (ClassCastException | ArithmeticException e) {
      //TODO: better error reporting upward to service response (NSPEx doesn't allow passing e as cause)
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * {@inheritDoc}
   *
   * retrieves the metadata via a single atomic graphql query
   */
  @Override
  public PlanMetadata getPlanMetadata(final String planId) throws NoSuchPlanException {
    final var request = (
        "query getPlanMetadata { "
        + "plan_by_pk( id: %s ) { "
        + "  id revision start_time duration "
        + "  mission_model { "
        + "    id name version "
        + "    uploaded_file { path } "
        + "    parameters { parameters } "
        + "  } "
        + "} }"
    ).formatted(planId);
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      //TODO: elevate and then leverage existing MerlinParsers (after updating them to match current db!)
      final var plan = response.getJsonObject("data").getJsonObject("plan_by_pk");
      final long planPK = plan.getJsonNumber("id").longValue();
      final long planRev = plan.getJsonNumber("revision").longValue();
      final var startTime = Timestamp.fromString(plan.getString("start_time"));
      final var duration = Interval.parse(plan.getString("duration"));

      final var model = plan.getJsonObject("mission_model");
      final var modelId = model.getJsonNumber("id").longValue();
      final var modelName = model.getString("name");
      final var modelVersion = model.getString("version");

      final var file = model.getJsonObject("uploaded_file");
      final var modelPath = Path.of(file.getString("path"));

      final var params = model.getJsonObject("parameters").getJsonObject("parameters");
      final var modelConfiguration = BasicParsers
          .mapP(new SerializedValueJsonParser()).parse(params)
          .getSuccessOrThrow((reason) -> new InvalidJsonException(new InvalidEntityException(List.of(reason))));

      //TODO: unify scheduler/aerie time types to avoid conversions
      final var endTime = new Timestamp((Instant) duration.addTo(startTime.toInstant()));
      final var horizon = new PlanningHorizon(
          Time.fromString(startTime.toString()),
          Time.fromString(endTime.toString()));

      return new PlanMetadata(
          planPK, planRev,
          horizon,
          modelId, modelPath, modelName, modelVersion,
          modelConfiguration);
    } catch (ClassCastException | ArithmeticException | InvalidJsonException e) {
      //TODO: better error reporting upward to service response (NSPEx doesn't allow passing e as cause)
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * create an in-memory snapshot of the target plan's activity contents from aerie
   *
   * @param planMetadata identifying details of the plan to fetch content for
   * @param mission the mission model that the plan adheres to
   * @return a newly allocated snapshot of the plan contents
   */
  public Plan getPlanActivities(final PlanMetadata planMetadata, final MissionModelWrapper mission) {
    //thanks to AMaillard for already having these handy!
    final var controller = new AerieController(
        this.merlinGraphqlURI.toString(), (int) planMetadata.modelId(), planMetadata.horizon(), mission);
    return controller.fetchPlan(planMetadata.planId());
  }

}
