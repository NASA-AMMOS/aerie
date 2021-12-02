package gov.nasa.jpl.aerie.scheduler.server.services;

import com.impossibl.postgres.api.data.Interval;
import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * {@inheritDoc}
 *
 * @param merlinGraphqlURI endpoint of the merlin graphql service that should be used to access all plan data
 */
public record GraphQLMerlinService(URI merlinGraphqlURI) implements MerlinService {

  private static final java.time.Duration httpTimeout = java.time.Duration.ofSeconds(60);

  //TODO: maybe use fancy aerie typed json parsers/serializers, ala BasicParsers.productP use in MerlinParsers
  //TODO: or upgrade to gson or similar modern library with registered object mappings

  /**
   * dispatch the given graphql request to aerie and collect the results
   *
   * absorbs any io errors and returns an empty response object in order to keep exception
   * signature of callers cleanly matching the MerlinService interface
   *
   * @param gqlStr the graphQL query or mutation to send to aerie
   * @return the json response returned by aerie, or an empty optional in case of io errors
   */
  private Optional<JSONObject> postRequest(String gqlStr) {
    //TODO: how severely to error out if aerie cannot be reached or has a 500 error etc?
    try {
      final var reqBody = new JSONObject().put("query", gqlStr).toString();
      final var httpReq = HttpRequest
          .newBuilder().uri(merlinGraphqlURI).timeout(httpTimeout)
          .header("Content-Type", "application/json")
          .header("Accept-Encoding", "gzip, deflate, br")
          .header("Accept", "application/json")
          .header("Origin", merlinGraphqlURI.toString())
          .POST(HttpRequest.BodyPublishers.ofString(reqBody))
          .build();
      final var httpResp = HttpClient
          .newHttpClient().send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
      if (httpResp.statusCode() != 200) {
        return Optional.empty();
      }
      //TODO: allow for streaming to json parser instead of loading entire body into memory (twice!)
      final var respBody = new String(new GZIPInputStream(httpResp.body()).readAllBytes());
      return Optional.of(new JSONObject(respBody));
    } catch (IOException | InterruptedException e) {
      //TODO: maybe retry if interrupted? but depends on semantics (eg don't duplicate mutation if not idempotent)
      return Optional.empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPlanRevision(final String planId) throws NoSuchPlanException {
    final var request = "query getPlanRevision { plan_by_pk( id: %s ) { revision } }"
        .formatted(planId);
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      return response.getJSONObject("data").getJSONObject("plan_by_pk").getLong("revision");
    } catch (JSONException e) {
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
      final var plan = response.getJSONObject("data").getJSONObject("plan_by_pk");
      final long planPK = plan.getLong("id");
      final long planRev = plan.getLong("revision");
      final var startTime = Timestamp.fromString(plan.getString("start_time"));
      final var duration = Interval.parse(plan.getString("duration"));

      final var model = plan.getJSONObject("mission_model");
      final var modelId = model.getLong("id");
      final var modelName = model.getString("name");
      final var modelVersion = model.getString("version");

      final var file = model.getJSONObject("uploaded_file");
      final var modelPath = Path.of(file.getString("path"));

      final var params = model.getJSONObject("parameters").getJSONObject("parameters");
      //TODO: would be nice to recycle map / SerVal parsers from merlin
      //BasicParsers.mapP(new SerializedValueJsonParser()).parse(new JsonValueparams);
      final var modelConfiguration = Map.<String, SerializedValue>of();

      //TODO: unify scheduler/aerie time types to avoid conversions
      final var endTime = new Timestamp((Instant)duration.addTo(startTime.toInstant()));
      final var horizon = new PlanningHorizon(
          Time.fromString(startTime.toString()),
          Time.fromString(endTime.toString()));

      return new PlanMetadata(
          planPK, planRev,
          horizon,
          modelId, modelPath, modelName, modelVersion,
          modelConfiguration);
    } catch (JSONException e) {
      throw new NoSuchPlanException(planId);
    }
  }


}
