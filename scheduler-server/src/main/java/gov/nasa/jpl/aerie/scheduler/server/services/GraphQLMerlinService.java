package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.BasicParsers;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.model.*;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.MerlinPlan;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;
import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLInterval;
import static gov.nasa.jpl.aerie.scheduler.server.graphql.GraphQLParsers.parseGraphQLTimestamp;

/**
 * {@inheritDoc}
 *
 * @param merlinGraphqlURI endpoint of the merlin graphql service that should be used to access all plan data
 */
public record GraphQLMerlinService(URI merlinGraphqlURI) implements PlanService.OwnerRole,
    MissionModelService
{

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
  protected Optional<JsonObject> postRequest(final String gqlStr) throws IOException, PlanServiceException {
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
        throw new PlanServiceException(respBody.toString());
      }
      return Optional.of(respBody);
    } catch (final InterruptedException e) {
      //TODO: maybe retry if interrupted? but depends on semantics (eg don't duplicate mutation if not idempotent)
      return Optional.empty();
    } catch (final JsonException e) { // or also JsonParsingException
      throw new IOException("json parse error on graphql response:" + e.getMessage(), e);
    }
  }

  protected Optional<JsonObject> postRequest(final String query, final JsonObject variables) throws IOException, PlanServiceException {
    try {
      //TODO: (mem optimization) use streams here to avoid several copies of strings
      final var reqBody = Json
          .createObjectBuilder()
          .add("query", query)
          .add("variables", variables)
          .build();
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
        throw new PlanServiceException(respBody.toString());
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
  public long getPlanRevision(final PlanId planId) throws IOException, NoSuchPlanException, PlanServiceException {
    final var query = """
        query GetPlanRevision($id: Int!) {
          plan_by_pk(id: $id) {
            revision
          }
        }
        """;
    final var variables = Json.createObjectBuilder().add("id", planId.id()).build();

    final var response = postRequest(query, variables).orElseThrow(() -> new NoSuchPlanException(planId));
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
  throws IOException, NoSuchPlanException, PlanServiceException
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
            .mapP(serializedValueP).parse(args)
            .getSuccessOrThrow((reason) -> new InvalidJsonException(new InvalidEntityException(List.of(reason))));
      }

      final var endTime = (Instant) duration.addTo(startTime.toInstant());
      final var horizon = new PlanningHorizon(startTime.toInstant(), endTime);

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
   * @return
   */
  @Override
  public MerlinPlan getPlanActivityDirectives(final PlanMetadata planMetadata, final Problem problem)
  throws IOException, NoSuchPlanException, PlanServiceException, InvalidJsonException, InstantiationException
  {
    final var merlinPlan = new MerlinPlan();
    final var request =
        "query { plan_by_pk(id:%d) { activity_directives { id start_offset type arguments anchor_id anchored_to_start } duration start_time }} ".formatted(
            planMetadata.planId().id());
    final var response = postRequest(request).orElseThrow(() -> new NoSuchPlanException(planMetadata.planId()));
    final var jsonplan = response.getJsonObject("data").getJsonObject("plan_by_pk");
    final var activityDirectives = jsonplan.getJsonArray("activity_directives");
    for (int i = 0; i < activityDirectives.size(); i++) {
      final var jsonActivity = activityDirectives.getJsonObject(i);
      final var type = activityDirectives.getJsonObject(i).getString("type");
      final var start = jsonActivity.getString("start_offset");
      final Integer anchorId = jsonActivity.isNull("anchor_id") ? null : jsonActivity.getInt("anchor_id");
      final boolean anchoredToStart = jsonActivity.getBoolean("anchored_to_start");
      final var arguments = jsonActivity.getJsonObject("arguments");
      final var deserializedArguments = BasicParsers
          .mapP(serializedValueP)
          .parse(arguments)
          .getSuccessOrThrow((reason) -> new InvalidJsonException(new InvalidEntityException(List.of(reason))));
      final var effectiveArguments = problem
          .getActivityType(type)
          .getSpecType()
          .getInputType()
          .getEffectiveArguments(deserializedArguments);
      final var merlinActivity = new ActivityDirective(
          Duration.of(parseGraphQLInterval(start).getDuration().toNanos() / 1000, Duration.MICROSECONDS),
          type,
          effectiveArguments,
          (anchorId != null) ? new ActivityDirectiveId(anchorId) : null,
          anchoredToStart);
      final var actPK = new ActivityDirectiveId(jsonActivity.getJsonNumber("id").longValue());
      merlinPlan.addActivity(actPK, merlinActivity);
    }
    return merlinPlan;
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
  public Pair<PlanId, Map<SchedulingActivityDirective, ActivityDirectiveId>> createNewPlanWithActivityDirectives(
      final PlanMetadata planMetadata,
      final Plan plan,
      final Map<SchedulingActivityDirective, GoalId> activityToGoalId
  )
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    final var planName = getNextPlanName();
    final var planId = createEmptyPlan(
        planName, planMetadata.modelId(),
        planMetadata.horizon().getStartInstant(), planMetadata.horizon().getEndAerie());
    //create sim storage space since doesn't happen automatically (else breaks further queries)
    createSimulationForPlan(planId);
    final Map<SchedulingActivityDirective, ActivityDirectiveId> activityToId = createAllPlanActivityDirectives(planId, plan, activityToGoalId);

    return Pair.of(planId, activityToId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanId createEmptyPlan(final String name, final long modelId, final Instant startTime, final Duration duration)
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    final var requestFormat = (
        "mutation createEmptyPlan { insert_plan_one( object: { "
        + "name: %s model_id: %d start_time: %s duration: %s "
        + "} ) { id } }");
    //TODO: resolve inconsistency in plan duration versus activity duration formats in merlin
    //NB: the duration format for creating plans is different than that for activity instances (microseconds)
    final var durStr = "\"" + duration.in(Duration.SECOND) + "\"";
    final var request = requestFormat.formatted(
        serializeForGql(name), modelId, serializeForGql(startTime.toString()), durStr);

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
  throws IOException, NoSuchPlanException, PlanServiceException
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
  public Map<SchedulingActivityDirective, ActivityDirectiveId> updatePlanActivityDirectives(
      final PlanId planId,
      final Map<SchedulingActivityDirectiveId, ActivityDirectiveId> idsFromInitialPlan,
      final MerlinPlan initialPlan,
      final Plan plan,
      final Map<SchedulingActivityDirective, GoalId> activityToGoalId
  )
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    final var ids = new HashMap<SchedulingActivityDirective, ActivityDirectiveId>();
    //creation are done in batch as that's what the scheduler does the most
    final var toAdd = new ArrayList<SchedulingActivityDirective>();
    for (final var activity : plan.getActivities()) {
      if(activity.getParentActivity().isPresent()) continue; // Skip generated activities
      final var idActFromInitialPlan = idsFromInitialPlan.get(activity.getId());
      if (idActFromInitialPlan != null) {
        //add duration to parameters if controllable
        if (activity.getType().getDurationType() instanceof DurationType.Controllable durationType){
          if (!activity.arguments().containsKey(durationType.parameterName())){
            activity.addArgument(durationType.parameterName(), SerializedValue.of(activity.duration().in(Duration.MICROSECONDS)));
          }
        }
        final var actFromInitialPlan = initialPlan.getActivityById(idActFromInitialPlan);
        //if act was present in initial plan
        final var activityDirectiveFromSchedulingDirective = new ActivityDirective(
            activity.startOffset(),
            activity.type().getName(),
            activity.arguments(),
            (activity.anchorId() != null ? new ActivityDirectiveId(-activity.anchorId().id()) : null),
            activity.anchoredToStart()
        );
        final var activityDirectiveId = idsFromInitialPlan.get(activity.getId());
        if (!activityDirectiveFromSchedulingDirective.equals(actFromInitialPlan.get())) {
          throw new PlanServiceException("The scheduler should not be updating activity instances");
          //updateActivityDirective(planId, schedulerActIntoMerlinAct, activityDirectiveId, activityToGoalId.get(activity));
        }
        ids.put(activity, activityDirectiveId);
      } else {
        //act was not present in initial plan, create new activity
        toAdd.add(activity);
      }
    }
    final var actsFromNewPlan = plan.getActivitiesById();
    for (final var idActInInitialPlan : idsFromInitialPlan.entrySet()) {
      if (!actsFromNewPlan.containsKey(idActInInitialPlan.getKey())) {
        throw new PlanServiceException("The scheduler should not be deleting activity instances");
        //deleteActivityDirective(idActInInitialPlan.getValue());
      }
    }

    //Create
    ids.putAll(createActivityDirectives(planId, toAdd, activityToGoalId));

    return ids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void ensurePlanExists(final PlanId planId) throws IOException, NoSuchPlanException, PlanServiceException {
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
  public void clearPlanActivityDirectives(final PlanId planId) throws IOException, NoSuchPlanException, PlanServiceException {
    ensurePlanExists(planId);
    final var request = (
        "mutation clearPlanActivities {"
        + "  delete_activity_directive(where: { plan_id: { _eq: %d } }) {"
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
  public Map<SchedulingActivityDirective, ActivityDirectiveId> createAllPlanActivityDirectives(
      final PlanId planId,
      final Plan plan,
      final Map<SchedulingActivityDirective, GoalId> activityToGoalId
  )
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    return createActivityDirectives(planId, plan.getActivitiesByTime(), activityToGoalId);
  }

  public Map<SchedulingActivityDirective, ActivityDirectiveId> createActivityDirectives(
      final PlanId planId,
      final List<SchedulingActivityDirective> orderedActivities,
      final Map<SchedulingActivityDirective, GoalId> activityToGoalId
  )
  throws IOException, NoSuchPlanException, PlanServiceException
  {
    ensurePlanExists(planId);
    final var query = """
        mutation createAllPlanActivityDirectives($activities: [activity_directive_insert_input!]!) {
          insert_activity_directive(objects: $activities) {
            returning {
              id
            }
            affected_rows
          }
        }
        """;

    //assemble the entire mutation request body
    //TODO: (optimization) could use a lazy evaluating stream of strings to avoid large set of strings in memory
    //TODO: (defensive) should sanitize all strings uses as keys/values to avoid injection attacks

    final var insertionObjects = Json.createArrayBuilder();
    for (final var act : orderedActivities) {
      var insertionObject = Json
          .createObjectBuilder()
          .add("plan_id", planId.id())
          .add("type", act.getType().getName())
          .add("start_offset", act.startOffset().toString());

      //add duration to parameters if controllable
      final var insertionObjectArguments = Json.createObjectBuilder();
      if(act.getType().getDurationType() instanceof DurationType.Controllable durationType){
        if(!act.arguments().containsKey(durationType.parameterName())){
          insertionObjectArguments.add(durationType.parameterName(), act.duration().in(Duration.MICROSECOND));
        }
      }

      final var goalId = activityToGoalId.get(act);
      if (goalId != null) {
        insertionObject.add("source_scheduling_goal_id", goalId.id());
      }

      for (final var arg : act.arguments().entrySet()) {
        //serializedValueP is safe to use here because only unparsing. otherwise subject to int/double typing confusion
        insertionObjectArguments.add(arg.getKey(), serializedValueP.unparse(arg.getValue()));
      }
      insertionObject.add("arguments", insertionObjectArguments.build());
      insertionObjects.add(insertionObject.build());
    }

    final var arguments = Json
        .createObjectBuilder()
        .add("activities", insertionObjects.build())
        .build();

    final var response = postRequest(query, arguments).orElseThrow(() -> new NoSuchPlanException(planId));

    final Map<SchedulingActivityDirective, ActivityDirectiveId> instanceToInstanceId = new HashMap<>();
    try {
      final var numCreated = response
          .getJsonObject("data").getJsonObject("insert_activity_directive").getJsonNumber("affected_rows").longValueExact();
      if (numCreated != orderedActivities.size()) {
        throw new NoSuchPlanException(planId);
      }
      var ids = response
          .getJsonObject("data").getJsonObject("insert_activity_directive").getJsonArray("returning");
      //make sure we associate the right id with the right activity
      for(int i = 0; i < ids.size(); i++) {
        instanceToInstanceId.put(orderedActivities.get(i), new ActivityDirectiveId(ids.getJsonObject(i).getInt("id")));
      }
    } catch (ClassCastException | ArithmeticException e) {
      throw new NoSuchPlanException(planId);
    }
    return instanceToInstanceId;
  }

  @Override
  public MissionModelTypes getMissionModelTypes(final PlanId planId)
  throws IOException, MissionModelServiceException
  {
    final var request = """
        query GetActivityTypesForPlan {
          plan_by_pk(id: %d) {
            mission_model {
              id
              activity_types {
                name
                parameters
              }
            }
          }
        }
        """.formatted(planId.id());
    final JsonObject response;
    try {
      response = postRequest(request).get();
    } catch (PlanServiceException e) {
      throw new MissionModelServiceException("Failed to get mission model types for plan id %s".formatted(planId), e);
    }
    final var activityTypesJsonArray =
        response.getJsonObject("data")
                .getJsonObject("plan_by_pk")
                .getJsonObject("mission_model")
                .getJsonArray("activity_types");
    final var activityTypes = parseActivityTypes(activityTypesJsonArray);

    final var missionModelId = new MissionModelId(response.getJsonObject("data")
                                       .getJsonObject("plan_by_pk")
                                       .getJsonObject("mission_model")
                                       .getInt("id"));

    return new MissionModelTypes(activityTypes, getResourceTypes(missionModelId));
  }

  private static List<ActivityType> parseActivityTypes(final JsonArray activityTypesJsonArray) {
    final var activityTypes = new ArrayList<ActivityType>();
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
      activityTypes.add(new ActivityType(activityTypeJson.asJsonObject().getString("name"), parameters));
    }
    return activityTypes;
  }

  @Override
  public MissionModelTypes getMissionModelTypes(final MissionModelId missionModelId)
  throws IOException, MissionModelServiceException, NoSuchMissionModelException
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
    final JsonObject response;
    try {
      response = postRequest(request).get();
    } catch (PlanServiceException e) {
      throw new MissionModelServiceException("Failed to get mission model types for model id %s".formatted(missionModelId), e);
    }
    final var data = response.getJsonObject("data");
    if (data.get("mission_model_by_pk").getValueType().equals(JsonValue.ValueType.NULL)) throw new NoSuchMissionModelException(missionModelId);
    final var activityTypesJsonArray = data
        .getJsonObject("mission_model_by_pk")
        .getJsonArray("activity_types");
    final var activityTypes = parseActivityTypes(activityTypesJsonArray);

    return new MissionModelTypes(activityTypes, getResourceTypes(missionModelId));
  }

  public Collection<ResourceType> getResourceTypes(final MissionModelId missionModelId)
  throws IOException, MissionModelServiceException
  {
    final var request = """
        query GetResourceTypes {
           resourceTypes(missionModelId: "%d") {
             name
             schema
           }
         }
        """.formatted(missionModelId.id());
    final JsonObject response;
    try {
      response = postRequest(request).get();
    } catch (PlanServiceException e) {
      throw new MissionModelServiceException("Failed to get mission model types for model id %s".formatted(missionModelId), e);
    }
    final var data = response.getJsonObject("data");
    final var resourceTypesJsonArray = data.getJsonArray("resourceTypes");

    final var resourceTypes = new ArrayList<ResourceType>();

    for (final var jsonValue : resourceTypesJsonArray) {
      final var jsonObject = jsonValue.asJsonObject();
      final var name = jsonObject.getString("name");
      final var schema = jsonObject.getJsonObject("schema");

      resourceTypes.add(new ResourceType(name, valueSchemaP.parse(schema).getSuccessOrThrow()));
    }

    return resourceTypes;
  }

  /**
   * serialize the given string in a manner that can be used as a graphql argument value
   * @param s the string to serialize
   * @return a serialization of the object suitable for use as a graphql value
   */
  public String serializeForGql(final String s) {
    //TODO: can probably leverage some serializers from aerie
    //TODO: (defensive) should escape contents of bare strings, eg internal quotes
    //NB: Time::toString will format correctly as HH:MM:SS.sss, just need to quote it here
    return "\"" + s + "\"";
  }
}
