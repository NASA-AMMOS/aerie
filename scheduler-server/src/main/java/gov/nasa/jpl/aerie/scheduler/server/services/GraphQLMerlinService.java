package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLInterval;
import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLTimestamp;

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
  private Optional<JsonObject> postRequest(final String gqlStr) throws IOException {
    try {
      //TODO: (mem optimization) use streams here to avoid several copies of strings
      final var reqBody = Json.createObjectBuilder().add("query", gqlStr).build();
      final var httpReq = HttpRequest
          .newBuilder().uri(merlinGraphqlURI).timeout(httpTimeout)
          .header("Content-Type", "application/json")
          .header("Accept-Encoding", "gzip, deflate, br")
          .header("Accept", "application/json")
          .header("Origin", merlinGraphqlURI.toString())
          .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
          .build();
      //TODO: (net optimization) gzip compress the request body if large enough (eg for createAllActs)
      final var httpResp = HttpClient
          .newHttpClient().send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
      if (httpResp.statusCode() != 200) {
        //TODO: how severely to error out if aerie cannot be reached or has a 500 error or json is garbled etc etc?
        return Optional.empty();
      }
      final var respBody = Json.createReader(new GZIPInputStream(httpResp.body())).readObject();
      return Optional.of(respBody);
    } catch (final InterruptedException e) {
      //TODO: maybe retry if interrupted? but depends on semantics (eg don't duplicate mutation if not idempotent)
      return Optional.empty();
    } catch (final JsonException e) { // or also JsonParsingException
      throw new IOException("json parse error on graphql response:" + e.getMessage(), e);
    }
  }

  //TODO: maybe use fancy aerie typed json parsers/serializers, ala BasicParsers.productP use in MerlinParsers
  //TODO: or upgrade to gson or similar modern library with registered object mappings

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPlanRevision(final String planId) throws IOException, NoSuchPlanException {
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
  public PlanMetadata getPlanMetadata(final String planId) throws IOException, NoSuchPlanException {
    final var request = (
        "query getPlanMetadata { "
        + "plan_by_pk( id: %s ) { "
        + "  id revision start_time duration "
        + "  mission_model { "
        + "    id name version "
        + "    uploaded_file { name } "
        + "  } "
        + "  simulations(limit:1, order_by:{revision:desc} ) { arguments }"
        + "} }"
    ).formatted(planId);
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      //TODO: elevate and then leverage existing MerlinParsers (after updating them to match current db!)
      final var plan = response.getJsonObject("data").getJsonObject("plan_by_pk");
      final long planPK = plan.getJsonNumber("id").longValue();
      final long planRev = plan.getJsonNumber("revision").longValue();
      final var startTime = parseGraphQLTimestamp(plan.getString("start_time"));
      final var duration = parseGraphQLInterval(plan.getString("duration"));

      final var model = plan.getJsonObject("mission_model");
      final var modelId = model.getJsonNumber("id").longValue();
      final var modelName = model.getString("name");
      final var modelVersion = model.getString("version");

      final var file = model.getJsonObject("uploaded_file");
      final var modelPath = Path.of(file.getString("name"));
      //NB: not using the "path" field because it is just a hex-encoded duplicate of the name field anyway
      //NB: the name includes the .jar extension

      //TODO: how to know right model config for scheduling? for now choosing latest sim setup (see query above)
      var modelConfiguration = Map.<String, SerializedValue>of();
      final var sims = plan.getJsonArray("simulations");
      if (!sims.isEmpty()) {
        final var args = sims.getJsonObject(0).getJsonObject("arguments");
        modelConfiguration = BasicParsers
            .mapP(new SerializedValueJsonParser()).parse(args)
            .getSuccessOrThrow((reason) -> new InvalidJsonException(new InvalidEntityException(List.of(reason))));
      }

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

  /**
   * create an entirely new plan container in aerie and synchronize the in-memory plan to it
   *
   * @param planMetadata identifying details of the plan to store content into; outdated on return
   * @param mission the mission model that the plan adheres to
   * @param plan plan with all activity instances that should be stored to target merlin plan container
   */
  public void createNewPlanWithActivities(
      final PlanMetadata planMetadata,
      final MissionModelWrapper mission,
      final Plan plan)
  {
    final var controller = new AerieController(
        this.merlinGraphqlURI.toString(), (int) planMetadata.modelId(), planMetadata.horizon(), mission);
    controller.initEmptyPlan(plan, planMetadata.horizon().getStartAerie(), planMetadata.horizon().getEndAerie(),
                             null);

    //create sim storage space since doesn't happen automatically (else breaks)
    //TODO: might expect that aerie creates any necessary extra containers for plans (as happens in UI sim)
    controller.createSimulation(plan);

    //TODO: (cleanup) sendPlan itself contains a duplicate CreatePlanRequest vs the initEmptyPlan call above
    controller.sendPlan(plan, planMetadata.horizon().getStartAerie(), planMetadata.horizon().getEndAerie(), null);
  }

  /**
   * synchronize the in-memory plan back over to aerie data stores via update operations
   *
   * the plan revision will change!
   *
   * @param planId aerie database identifier of the target plan to synchronize into
   * @param plan plan with all activity instances that should be stored to target merlin plan container
   */
  public void updatePlanActivities(final long planId, final Plan plan) throws IOException, NoSuchPlanException
  {
    //TODO: (api violation) currently clearing and repopulating plan; but loses existing activity instance ids!
    //TODO: (perf improvement) calculate or cache plan diffs during sched and then upload to aerie in batch here
    //TODO: (defensive) should combine all mutations into one graphql transaction to avoid intermediate mods
    clearPlanActivities(planId);
    createAllPlanActivities(planId, plan);
  }

  /**
   * confirms that the specified plan exists in the aerie database, throwing exception if not
   *
   * @param planId the target plan database identifier
   */
  //TODO: (defensive) should combine such checks into the mutations they are guarding, but not possible?
  public void ensurePlanExists(final long planId) throws IOException, NoSuchPlanException {
    final Supplier<NoSuchPlanException> exceptionFactory = () -> new NoSuchPlanException(Long.toString(planId));
    final var request = "query ensurePlanExists { plan_by_pk( id: %s ) { id } }"
        .formatted(planId);
    final var response = postRequest(request).orElseThrow(exceptionFactory);
    try {
      final var id = response.getJsonObject("data").getJsonObject("plan_by_pk").getJsonNumber("id").longValueExact();
      if (id != planId) {
        throw exceptionFactory.get();
      }
    } catch (ClassCastException | ArithmeticException e) {
      //TODO: better error reporting upward to service response (NSPEx doesn't allow passing e as cause)
      throw exceptionFactory.get();
    }

  }

  /**
   * delete all the activity instances stored in the target plan container
   *
   * the plan revision will change!
   *
   * @param planId the database id of the plan container to clear
   */
  //TODO: (error cleanup) more diverse exceptions for failed operations
  public void clearPlanActivities(final long planId) throws IOException, NoSuchPlanException {
    ensurePlanExists(planId);
    final var request = (
        "mutation clearPlanActivities {"
        + "  delete_activity(where: { plan_id: { _eq: %d } }) {"
        + "    affected_rows"
        + "  }"
        + "}"
    ).formatted(planId);
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(Long.toString(planId)));
    try {
      response.getJsonObject("data").getJsonObject("delete_activity").getJsonNumber("affected_rows").longValueExact();
    } catch (ClassCastException | ArithmeticException e) {
      //TODO: (error cleanup) better error reporting upward to service response (NSPEx doesn't allow passing cause)
      throw new NoSuchPlanException(Long.toString(planId));
    }
  }

  /**
   * create activity instances in the target plan container for each activity in the input plan
   *
   * does not attempt to resolve id clashes or do activity instance updates
   *
   * the plan revision will change!
   *
   * @param planId the database id of the plan container to populate with new activity instances
   * @param plan the plan from which to copy all activity instances into aerie
   */
  public void createAllPlanActivities(final long planId, final Plan plan) throws IOException, NoSuchPlanException {
    ensurePlanExists(planId);
    final var requestPre = "mutation createAllPlanActivities { insert_activity( objects: [";
    final var requestPost = "] ) { affected_rows } }";
    final var actPre = "{ plan_id: %d type: \"%s\" start_offset: \"%s\" arguments: {";
    final var actPost = "} }";
    final var argFormat = "%s: %s ";

    //assemble the entire mutation request body
    //TODO: (optimization) could use a lazy evaluating stream of strings to avoid large set of strings in memory
    //TODO: (defensive) should sanitize all strings uses as keys/values to avoid injection attacks
    final var requestSB = new StringBuilder().append(requestPre);
    for (final var act : plan.getActivities()) {
      requestSB.append(actPre.formatted(planId, act.getType().getName(), act.getStartTime().toString()));
      if (act.getDuration() != null) {
        requestSB.append(argFormat.formatted("duration", getGraphQLValueString(act.getDuration())));
      }
      for (final var arg : act.getParameters().entrySet()) {
        final var name = arg.getKey();
        var value = getGraphQLValueString(arg.getValue());
        requestSB.append(argFormat.formatted(name, value));
      }
      requestSB.append(actPost);
    }
    requestSB.append(requestPost);
    final var request = requestSB.toString();

    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(Long.toString(planId)));
    try {
      final var numCreated = response
          .getJsonObject("data").getJsonObject("insert_activity").getJsonNumber("affected_rows").longValueExact();
      if (numCreated != plan.getActivities().size()) {
        throw new NoSuchPlanException(Long.toString(planId));
      }
    } catch (ClassCastException | ArithmeticException e) {
      //TODO: (error cleanup) better error reporting upward to service response (NSPEx doesn't allow passing cause)
      throw new NoSuchPlanException(Long.toString(planId));
    }
  }

  /**
   * serialize the given java object in a manner that can be used as a graphql argument value
   *
   * eg wraps strings or enums in quotes
   *
   * @param obj the object to serialize
   * @return a serialization of the object suitable for use as a graphql value
   */
  public String getGraphQLValueString(Object obj) {
    //TODO: can probably leverage some serializers from aerie
    if (obj instanceof String || obj instanceof Enum<?> || obj instanceof Time) {
      //TODO: (defensive) should escape contents of bare strings, eg internal quotes
      //NB: Time::toString will format correctly as HH:MM:SS.sss, just need to quote it here
      return "\"" + obj + "\"";
    } else if (obj instanceof Duration dur) {
      //NB: merlin uses durations in microseconds! (inconsistent with start_offset as a HH:MM:SS.sss string)
      return Long.toString(dur.in(Duration.MICROSECOND));
    } else {
      return obj.toString();
    }
  }


}
