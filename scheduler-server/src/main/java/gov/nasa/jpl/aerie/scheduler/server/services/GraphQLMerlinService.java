package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.AerieController;
import gov.nasa.jpl.aerie.scheduler.Plan;
import gov.nasa.jpl.aerie.scheduler.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.Problem;
import gov.nasa.jpl.aerie.scheduler.Time;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.http.SerializedValueJsonParser;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
import gov.nasa.jpl.aerie.scheduler.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLInterval;
import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLTimestamp;
import static gov.nasa.jpl.aerie.scheduler.server.http.ValueSchemaJsonParser.valueSchemaP;

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
  private Optional<JsonObject> postRequest(final String gqlStr) throws IOException, MerlinServiceException {
    try {
      //TODO: (mem optimization) use streams here to avoid several copies of strings
      final var reqBody = Json.createObjectBuilder().add("query", gqlStr).build();
      final var httpReq = HttpRequest
          .newBuilder().uri(merlinGraphqlURI).timeout(httpTimeout)
          .header("Content-Type", "application/json")
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
      final var respBody = Json.createReader(httpResp.body()).readObject();
      if (respBody.containsKey("errors")) {
        throw new MerlinServiceException(respBody.toString());
      }
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
  public long getPlanRevision(final PlanId planId) throws IOException, NoSuchPlanException, MerlinServiceException {
    final var request = "query getPlanRevision { plan_by_pk( id: %s ) { revision } }"
        .formatted(planId.id());
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      return response.getJsonObject("data").getJsonObject("plan_by_pk").getJsonNumber("revision").longValueExact();
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * {@inheritDoc}
   *
   * retrieves the metadata via a single atomic graphql query
   */
  @Override
  public PlanMetadata getPlanMetadata(final PlanId planId)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
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
    ).formatted(planId.id());
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
          new PlanId(planPK),
          planRev,
          horizon,
          modelId,
          modelPath,
          modelName,
          modelVersion,
          modelConfiguration);
    } catch (ClassCastException | ArithmeticException | InvalidJsonException e) {
      //TODO: better error reporting upward to service response (NSPEx doesn't allow passing e as cause)
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Plan getPlanActivities(final PlanMetadata planMetadata, final Problem problem)
  throws IOException, NoSuchPlanException
  {
    //thanks to AMaillard for already having these handy!
    final var controller = new AerieController(
        this.merlinGraphqlURI.toString(), (int) planMetadata.modelId(), planMetadata.horizon(), problem.getActivityTypes());
    return controller.fetchPlan(planMetadata.planId().id());
  }

  /**
   * generate a name for the next created plan container using current timestamp
   *
   * currently, does not actually verify that the name is unique within aerie database
   *
   * @return a name for the next created plan container
   */
  public String getNextPlanName() {
    //TODO: (defensive) should rely on database to generate a new unique name to avoid user collisions
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    return "scheduled_plan_" + dtf.format(LocalDateTime.now());
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Pair<PlanId, Map<ActivityInstance, ActivityInstanceId>> createNewPlanWithActivities(final PlanMetadata planMetadata, final Plan plan)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
    final var planName = getNextPlanName();
    final var planId = createEmptyPlan(
        planName, planMetadata.modelId(),
        planMetadata.horizon().getStartHuginn(), planMetadata.horizon().getEndAerie());
    //create sim storage space since doesn't happen automatically (else breaks further queries)
    createSimulationForPlan(planId);
    Map<ActivityInstance, ActivityInstanceId> activityToId = createAllPlanActivities(planId, plan);

    return Pair.of(planId, activityToId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanId createEmptyPlan(final String name, final long modelId, final Time startTime, final Duration duration)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
    final var requestFormat = (
        "mutation createEmptyPlan { insert_plan_one( object: { "
        + "name: %s model_id: %d start_time: %s duration: %s "
        + "} ) { id } }");
    //TODO: resolve inconsistency in plan duration versus activity duration formats in merlin
    //NB: the duration format for creating plans is different than that for activity instances (microseconds)
    final var durStr = "\"" + duration.in(Duration.SECOND) + "\"";
    final var request = requestFormat.formatted(
        getGraphQLValueString(name), modelId, getGraphQLValueString(startTime), durStr);

    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(null));
    try {
      return new PlanId(
          response
              .getJsonObject("data")
              .getJsonObject("insert_plan_one")
              .getJsonNumber("id")
              .longValueExact());
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(null);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createSimulationForPlan(final PlanId planId)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
    final var request = (
        "mutation createSimulationForPlan { insert_simulation_one( object: {"
        + "plan_id: %d arguments: {} } ) { id } }")
        .formatted(planId.id());
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      response.getJsonObject("data").getJsonObject("insert_simulation_one").getJsonNumber("id").longValueExact();
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Map<ActivityInstance, ActivityInstanceId> updatePlanActivities(final PlanId planId, final Plan plan)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
    //TODO: (api violation) currently clearing and repopulating plan; but loses existing activity instance ids!
    //TODO: (perf improvement) calculate or cache plan diffs during sched and then upload to aerie in batch here
    //TODO: (defensive) should combine all mutations into one graphql transaction to avoid intermediate mods
    clearPlanActivities(planId);
    return createAllPlanActivities(planId, plan);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void ensurePlanExists(final PlanId planId) throws IOException, NoSuchPlanException, MerlinServiceException {
    final Supplier<NoSuchPlanException> exceptionFactory = () -> new NoSuchPlanException(planId);
    final var request = "query ensurePlanExists { plan_by_pk( id: %s ) { id } }"
        .formatted(planId.id());
    final var response = postRequest(request).orElseThrow(exceptionFactory);
    try {
      final var id =
          new PlanId(
              response
              .getJsonObject("data")
              .getJsonObject("plan_by_pk")
              .getJsonNumber("id")
              .longValueExact());
      if (!id.equals(planId)) {
        throw exceptionFactory.get();
      }
    } catch (ClassCastException | ArithmeticException e) {
      //TODO: better error reporting upward to service response (NSPEx doesn't allow passing e as cause)
      throw exceptionFactory.get();
    }
  }

  /**
   * {@inheritDoc}
   */
  //TODO: (error cleanup) more diverse exceptions for failed operations
  @Override
  public void clearPlanActivities(final PlanId planId) throws IOException, NoSuchPlanException, MerlinServiceException {
    ensurePlanExists(planId);
    final var request = (
        "mutation clearPlanActivities {"
        + "  delete_activity(where: { plan_id: { _eq: %d } }) {"
        + "    affected_rows"
        + "  }"
        + "}"
    ).formatted(planId.id());
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      response.getJsonObject("data").getJsonObject("delete_activity").getJsonNumber("affected_rows").longValueExact();
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(planId);
    }
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Map<ActivityInstance, ActivityInstanceId> createAllPlanActivities(final PlanId planId, final Plan plan)
  throws IOException, NoSuchPlanException, MerlinServiceException
  {
    ensurePlanExists(planId);
    final var requestPre = "mutation createAllPlanActivities { insert_activity( objects: [";
    final var requestPost = "] ) { returning { id } affected_rows } }";
    final var actPre = "{ plan_id: %d type: \"%s\" start_offset: \"%s\" arguments: {";
    final var actPost = "} }";
    final var argFormat = "%s: %s ";

    //assemble the entire mutation request body
    //TODO: (optimization) could use a lazy evaluating stream of strings to avoid large set of strings in memory
    //TODO: (defensive) should sanitize all strings uses as keys/values to avoid injection attacks
    final var requestSB = new StringBuilder().append(requestPre);

    Map<ActivityInstance, ActivityInstanceId> instanceToInstanceId = new HashMap<>();
    var orderedActivities = plan.getActivities().stream().toList();

    for (final var act : orderedActivities) {
      requestSB.append(actPre.formatted(planId.id(), act.getType().getName(), act.getStartTime().toString()));
      for (final var arg : act.getArguments().entrySet()) {
        final var name = arg.getKey();
        var value = getGraphQLValueString(arg.getValue());
        requestSB.append(argFormat.formatted(name, value));
      }
      requestSB.append(actPost);
    }
    requestSB.append(requestPost);
    final var request = requestSB.toString();

    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planId));
    try {
      final var numCreated = response
          .getJsonObject("data").getJsonObject("insert_activity").getJsonNumber("affected_rows").longValueExact();
      if (numCreated != plan.getActivities().size()) {
        throw new NoSuchPlanException(planId);
      }
      var ids = response
          .getJsonObject("data").getJsonObject("insert_activity").getJsonArray("returning");
      //make sure we associate the right id with the right activity
      for(int i = 0; i < ids.size(); i++) {
        instanceToInstanceId.put(orderedActivities.get(i), new ActivityInstanceId(ids.getJsonObject(i).getInt("id")));
      }
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(planId);
    }
    return instanceToInstanceId;
  }

  @Override
  public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final PlanId planId)
  throws IOException, MerlinServiceException
  {
    final var request = """
        query GetActivityTypesForPlan {
          plan_by_pk(id: %d) {
            mission_model {
              activity_types {
                name
                parameters
              }
            }
          }
        }
        """.formatted(planId.id());
    final var response = postRequest(request).get();
    final var activityTypesJsonArray =
        response.getJsonObject("data")
                .getJsonObject("plan_by_pk")
                .getJsonObject("mission_model")
                .getJsonArray("activity_types");
    final var activityTypes = parseActivityTypes(activityTypesJsonArray);
    return new TypescriptCodeGenerationService.MissionModelTypes(activityTypes, List.of());
  }

  private static List<TypescriptCodeGenerationService.ActivityType> parseActivityTypes(final JsonArray activityTypesJsonArray) {
    final var activityTypes = new ArrayList<TypescriptCodeGenerationService.ActivityType>();
    for (final var activityTypeJson : activityTypesJsonArray) {
      final var parametersJson = activityTypeJson.asJsonObject().getJsonObject("parameters");
      final var parameters = new HashMap<String, ValueSchema>();
      for (final var parameterJson : parametersJson.entrySet()) {
        parameters.put(
            parameterJson.getKey(),
            valueSchemaP
                .parse(
                    parameterJson
                        .getValue()
                        .asJsonObject()
                        .getJsonObject("schema"))
                .getSuccessOrThrow());
      }
      activityTypes.add(new TypescriptCodeGenerationService.ActivityType(activityTypeJson.asJsonObject().getString("name"), parameters));
    }
    return activityTypes;
  }

  @Override
  public TypescriptCodeGenerationService.MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
  throws IOException, MerlinServiceException, NoSuchMissionModelException
  {
    final var request = """
        query GetActivityTypesFromMissionModel{
           mission_model_by_pk(id:%d){
             activity_types{
               name
               parameters
             }
           }
        }
        """.formatted(missionModelId.id());
    final var response = postRequest(request).get();
    final var data = response.getJsonObject("data");
    if (data.get("mission_model_by_pk").getValueType().equals(JsonValue.ValueType.NULL)) throw new NoSuchMissionModelException(missionModelId);
    final var activityTypesJsonArray = data
        .getJsonObject("mission_model_by_pk")
        .getJsonArray("activity_types");
    final var activityTypes = parseActivityTypes(activityTypesJsonArray);
    return new TypescriptCodeGenerationService.MissionModelTypes(activityTypes, List.of());
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
