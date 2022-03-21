package gov.nasa.jpl.aerie.scheduler;


import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 *
 */
public class AerieController {

  private static final Logger logger = LoggerFactory.getLogger(AerieController.class);

  private static final boolean DEBUG = true;
  private static final String IT_PLAN_BASENAME = "Plan_";
  private static final java.time.Duration TIMEOUT_HTTP = java.time.Duration.ofMinutes(10);

  //TODO: reconcile with aerie data model (eg different plans cannot hold the same activity instances, but scheduler plans can)
  private final Map<Plan, Long> planIds;
  private final Map<Plan, Map<String, AerieStateCache>> stateCaches;
  private final Map<Plan, Duration> planStartTimes;
  private final Map<String, ActivityType> activityTypes;


  private final Map<Plan, Map<String, Class<?>>> stateTypes;

  private final Map<ActivityInstance, Long> activityInstancesIds;
  private final int AMMOS_MISSION_MODEL_ID;
  private final String distantAerieURL;
  private final String plan_prefix;
  private final PlanningHorizon planningHorizon;

  //AUTHENTICATION
  private boolean authenticationRequired = false;
  private String username = null;
  private String password = null;
  private String authorizationHeader = null;


  public AerieController(
      final String distantAerieURL,
      final int missionModelId,
      final boolean authenticationRequired,
      final PlanningHorizon planningHorizon,
      final Collection<ActivityType> activityTypes)
  {
    this(distantAerieURL, missionModelId, planningHorizon, activityTypes);
    this.authenticationRequired = authenticationRequired;
  }

  public AerieController(
      final String distantAerieURL,
      final int missionModelId,
      final PlanningHorizon planningHorizon,
      final Collection<ActivityType> activityTypes)
  {
    this(distantAerieURL, missionModelId, "", planningHorizon, activityTypes);
  }

  public AerieController(
      final String distantAerieURL,
      final int missionModelId,
      final String planPrefix,
      final PlanningHorizon planningHorizon,
      final Collection<ActivityType> activityTypes)
  {
    this.activityTypes = activityTypes.stream().collect(Collectors.toMap(ActivityType::getName, at -> at));
    this.distantAerieURL = distantAerieURL;
    this.AMMOS_MISSION_MODEL_ID = missionModelId;
    planIds = new HashMap<>();
    stateCaches = new HashMap<>();
    stateTypes = new HashMap<>();
    activityInstancesIds = new HashMap<>();
    planStartTimes = new HashMap<>();
    plan_prefix = planPrefix;
    this.planningHorizon = planningHorizon;
  }


  public Duration fromAerieTime(String aerieTime) {
    return Time.fromString(aerieTime.substring(0, aerieTime.length() - 3), planningHorizon);
  }

  private Long getActivityInstanceId(ActivityInstance act) {
    return activityInstancesIds.get(act);
  }

  public Long getPlanId(Plan plan) {
    return planIds.get(plan);
  }

  private String createPlanName() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    LocalDateTime localDate = LocalDateTime.now();
    String todayDate = dtf.format(localDate);
    return plan_prefix + IT_PLAN_BASENAME + todayDate;
  }

  public void deleteAllMissionModelsBut(String notThisOne) {
    GetAllMissionModelsId getReq = new GetAllMissionModelsId();
    postRequest(getReq);
    List<String> ids = getReq.getMissionModelIds();
    for (var id : ids) {
      if (!id.equals(notThisOne)) {
        DeleteMissionModel da = new DeleteMissionModel(id);
        postRequest(da);
      }
    }
  }

  public Plan fetchPlan(final long planId) {
    final var req = new FetchPlanRequest(planId);
    postRequest(req);
    return req.getPlan();
  }

  protected boolean isSessionAlive() {
    SessionAliveRequest sar = new SessionAliveRequest();
    postRequest(sar);
    boolean isSessionAlive = true;
    return isSessionAlive;
  }

  private boolean currentlyTryingToConnect = false;

  protected void authenticateIfNecessary() {
    currentlyTryingToConnect = true;
    if (authenticationRequired) {
      if (!isSessionAlive()) {
        LoginRequest loginRequest = new LoginRequest();
        postRequest(loginRequest);

      }
    }
    currentlyTryingToConnect = false;
  }

  public boolean isLocalAerieUp() {
    boolean available = false;
    try {
      final URLConnection connection = new URL(this.distantAerieURL).openConnection();
      connection.connect();
      available = true;
    } catch (final MalformedURLException e) {
      throw new IllegalStateException("URL is incorrect: " + this.distantAerieURL, e);
    } catch (final IOException e) {
      available = false;
    }
    return available;
  }

  protected boolean postRequest(GraphRequest request) {
    if (!currentlyTryingToConnect) {
      authenticateIfNecessary();
    }
    String jsonString = request.getRequest();
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("query", jsonString);

    logger.debug(jsonString);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                                                        .uri(URI.create(distantAerieURL));

    httpRequestBuilder = httpRequestBuilder.timeout(TIMEOUT_HTTP)
                                           .header("Content-Type", "application/json")
                                           .header("Accept-Encoding", "gzip, deflate, br")
                                           .header("Accept", "application/json")
                                           .header("DNT", "1")
                                           .header("Origin", distantAerieURL)
                                           //.header("Content-Type", "application/graphql")
                                           .POST(HttpRequest.BodyPublishers.ofString(jsonObj.toString()));
    if (authorizationHeader != null) {
      httpRequestBuilder.header("authorization", authorizationHeader);
    }
    HttpRequest httpRequest = httpRequestBuilder.build();
    HttpResponse<InputStream> response = null;
    try {
      response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
    } catch (IOException | InterruptedException e) {
      StackTraceLogger.log(e, logger);
    }
    assert response != null;
    logger.info("Response code: " + response.statusCode());
    if (response.statusCode() != 200) {
      return false;
    } else {
      try {
        GZIPInputStream body = new GZIPInputStream(response.body());
        Reader reader = new InputStreamReader(body, StandardCharsets.UTF_8);
        Writer writer = new StringWriter();
        char[] buffer = new char[10240];
        int length = 0;
        while ((length = reader.read(buffer)) > 0) {
          writer.write(buffer, 0, length);
        }
        String bodystr = writer.toString();
        logger.info(bodystr);
        JSONObject json = new JSONObject(bodystr);
        return request.handleResponse(json);
      } catch (ZipException e) {
        //probably not in GZIP format
        StackTraceLogger.log(e, logger);
        String result = new BufferedReader(new InputStreamReader(response.body()))
            .lines().collect(Collectors.joining("\n"));
        logger.error("Response body: " + result);
        return false;
      } catch (IOException e) {
        StackTraceLogger.log(e, logger);
        return false;
      }
    }
  }


  public void deleteAllPlans() {
    PlanIdRequest pir = new PlanIdRequest();
    boolean ret = postRequest(pir);
    for (var id : pir.getIds()) {
      DeletePlanRequest dpr = new DeletePlanRequest(id);
      ret = postRequest(dpr);
    }
  }

  protected void addPlanId(Plan plan, Long id) {
    this.planIds.put(plan, id);
  }

  private void addActInstanceId(ActivityInstance act, Long id) {
    this.activityInstancesIds.put(act, id);
  }


  public boolean deletePlan(Plan plan) {
    boolean ret = true;
    if (planIds.get(plan) == null) {
      ret = false;
      logger.warn("Plan has never been sent to Aerie");
    } else {
      DeletePlanRequest deletePlanRequest = new DeletePlanRequest(plan);
      ret = postRequest(deletePlanRequest);
    }
    return ret;
  }


  public boolean updateActivity(ActivityInstance act, Plan plan) {
    boolean ret = true;
    if (planIds.get(plan) == null || activityInstancesIds.get(act) == null) {
      ret = false;
      logger.error("Plan or act have never been sent to Aerie");
    } else {
      UpdateInstanceRequest updateActivityInstanceRequest = new UpdateInstanceRequest(act, plan);
      ret = postRequest(updateActivityInstanceRequest);
    }
    return ret;
  }

  public boolean simulatePlan(Plan plan) {
    boolean ret = true;
    if (planIds.get(plan) == null) {
      ret = false;
      logger.error("Plan have never been sent to Aerie");
    } else {
      SimulatePlanRequest spr = new SimulatePlanRequest(plan);
      ret = postRequest(spr);
    }
    return ret;
  }

  public boolean deleteActivity(ActivityInstance act, Plan plan) {
    boolean ret = true;
    if (planIds.get(plan) == null || activityInstancesIds.get(act) == null) {
      ret = false;
      logger.warn("Plan or act have never been sent to Aerie");
    } else {
      DeleteActivityInstanceRequest deleteActivityInstanceRequest = new DeleteActivityInstanceRequest(act, plan);
      ret = postRequest(deleteActivityInstanceRequest);
    }
    return ret;
  }

  public boolean initEmptyPlan(
      Plan plan,
      Duration horizonBegin,
      Duration horizonEnd,
      Map<String, AerieStateCache> cache)
  {
    boolean ret = true;

    CreatePlanRequest planRequest = new CreatePlanRequest(plan, horizonBegin, horizonEnd, cache);
    boolean result = postRequest(planRequest);
    if (!result) {
      ret = false;
    }

    planStartTimes.put(plan, horizonBegin);
    return ret;
  }


  public boolean sendPlan(Plan plan, Duration horizonBegin, Duration horizonEnd, Map<String, AerieStateCache> cache) {
    boolean ret = true;
    var planId = planIds.get(plan);
    boolean result = true;
    if (planId == null) {
      CreatePlanRequest planRequest = new CreatePlanRequest(plan, horizonBegin, horizonEnd, cache);
      result = postRequest(planRequest);
    }
    if (!result) {
      ret = false;
    } else {
      for (ActivityInstance act : plan.getActivitiesByTime()) {
        //if(act.getType().getName().equals("Window")) continue;
        CreateActivityInstanceRequest actInstRequest = new CreateActivityInstanceRequest(act, plan);
        boolean res = postRequest(actInstRequest);
        if (!res) {
          ret = false;
          break;
        }
      }
    }
    planStartTimes.put(plan, horizonBegin);

    return ret;

  }

  /**
   * update the target plan container in merlin to be consistent with activities in the provided plan object
   *
   * abandons the plan update as soon as it encounters any failure
   *
   * @param planId the id of the plan to update in merlin
   * @param plan the activity contents to store into merlin
   * @return true iff all necessary updates and insertions succeeded
   */
  public boolean updatePlan(final long planId, final Plan plan) {
    //NB: it appears this code is out of date with the available hasura db mutations!
    //TODO: more efficient if we could do a batch-update/insert of all acts at once
    if (planIds.get(plan) == null) {
      logger.error("Plan has never been sent to Aerie");
      return false;
    } else {
      for (final var act : plan.getActivities()) {
        final boolean success;
        if (activityInstancesIds.containsKey(act)) {
          success = updateActivity(act, plan);
        } else {
          success = sendActivityInstance(plan, act);
        }
        if (!success) return false;
      }
    }
    return true;
  }

  public boolean sendActivityInstance(Plan plan, ActivityInstance act) {
    CreateActivityInstanceRequest actInstRequest = new CreateActivityInstanceRequest(act, plan);
    return postRequest(actInstRequest);
  }

  public Object getDoubleValue(Plan plan, String nameState, Duration t) {
    if (this.planIds.get(plan) != null) {
      var statesCache = this.stateCaches.get(plan);
      var statesTypes = this.stateTypes.get(plan);
      var stateCache = statesCache.get(nameState);

      Duration startPlan = planStartTimes.get(plan);

      if (stateCache == null) {
        simulatePlan(plan);
        stateCache = statesCache.get(nameState);
      }

      if (stateCache != null) {
        var value = stateCache.getValue(t, statesTypes.get(nameState));
        if (value != null) {
          return value;
        } else {
          simulatePlan(plan);
          value = stateCache.getValue(t, statesTypes.get(nameState));
          if (value == null) {
            throw new IllegalArgumentException("Even after resimulation, values cannot be computed");
          }
        }
      } else {
        logger.error("Required state with name " + nameState + " has NOT been found in the definition");
      }

    }

    return null;
  }

  protected abstract static class GraphRequest {

    public abstract String getRequest();

    public abstract boolean handleResponse(JSONObject response);


  }

  protected class FetchPlanRequest extends GraphRequest {

    public String getRequest() {
      var req = "query { plan_by_pk(id:%d) { activities { id start_offset type arguments } duration start_time }} ";
      return String.format(req, id);
    }

    public FetchPlanRequest(long id) {
      this.id = id;
    }

    Plan plan;
    final long id;

    public Plan getPlan() {
      return plan;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      var jsonplan = ((JSONObject) response.get("data")).getJSONObject("plan_by_pk");
      var activities = jsonplan.getJSONArray("activities");
      this.plan = new PlanInMemory();
      addPlanId(plan, this.id);
      for (int i = 0; i < activities.length(); i++) {
        var actInst = jsonToInstance(activities.getJSONObject(i));
        this.plan.add(actInst);
      }
      return true;
    }
  }

  private ActivityInstance jsonToInstance(JSONObject jsonActivity) {
    String type = jsonActivity.getString("type");
    if(!activityTypes.containsKey(type)){
      throw new IllegalArgumentException("Activity type found in JSON object after request to merlin server has not been found in types extracted from mission model. Probable inconsistency between mission model used by scheduler server and merlin server.");
    }
    var schedulerActType = activityTypes.get(type);
    ActivityInstance act = new ActivityInstance(schedulerActType);
    final var actPK = jsonActivity.getLong("id");
    addActInstanceId(act, actPK);

    String start = jsonActivity.getString("start_offset");
    var arguments = jsonActivity.getJSONObject("arguments");
    for (var paramName : arguments.keySet()) {
      var visitor = new DemuxJson(paramName, arguments);
      var paramSpec = ActivityType.getParameterSpecification(schedulerActType.getSpecType().getParameters(), paramName);
      assert(paramSpec != null);
      var valueParam = paramSpec.schema().match(visitor);
      act.addArgument(paramName, valueParam);
    }
    act.setStartTime(DemuxJson.fromString(start));

    return act;
  }

  protected static class PlanIdRequest extends GraphRequest {

    public String getRequest() {
      return "query { plan { id} } ";
    }

    final ArrayList<Long> idsL = new ArrayList<>();

    public ArrayList<Long> getIds() {
      return idsL;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      JSONArray ids = ((JSONObject) response.get("data")).getJSONArray("plan");
      for (int i = 0; i < ids.length(); i++) {
        idsL.add(((JSONObject) ids.get(i)).getLong("id"));
      }
      return true;
    }
  }


  private static final Map<String, Class<?>> AERIETYPE_TO_JAVATYPE = Map.ofEntries(
      Map.entry("double", Double.class),
      Map.entry("integer", Integer.class)

  );

  protected class StateTypesRequest extends GraphRequest {
    final Plan plan;

    public StateTypesRequest(Plan plan) {
      this.plan = plan;
    }

    public String getRequest() {
      return "query ResourceTypes { stateTypes( missionModelId: \""
             + AMMOS_MISSION_MODEL_ID
             + "\"){ name schema }}";
    }

    @Override
    public boolean handleResponse(JSONObject response) {

      var map = stateTypes.computeIfAbsent(plan, k -> new HashMap<>());


      JSONObject data = (JSONObject) response.get("data");
      JSONArray stateTypes = data.getJSONArray("stateTypes");

      for (int i = 0; i < stateTypes.length(); i++) {
        JSONObject type = stateTypes.getJSONObject(i);
        String name = type.getString("name");
        String aerieType = ((JSONObject) type.get("schema")).getString("type");
        map.put(name, AERIETYPE_TO_JAVATYPE.get(aerieType));
      }

      return true;
    }
  }

  protected class LoginRequest extends GraphRequest {

    public String getRequest() {
      if (username == null || password == null) {
        getCredentialsFromUser();
      }

      return "{\", username:\""
             + username
             + "\", password:\"}"
             + password;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      authorizationHeader =
          ((JSONObject) (((JSONObject) response.get("data")).get("login"))).getString("ssoCookieValue");
      return true;
    }
  }

  protected static class LogoutRequest extends GraphRequest {

    public String getRequest() {
      return "mutation { logout { message success }}";
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return true;
    }
  }

  protected static class DeleteMissionModel extends GraphRequest {

    final String delete;

    public DeleteMissionModel(String id) {
      delete = id;
    }

    public String getRequest() {
      return "mutation { deleteMissionModel(  id :\""
             + delete
             + "\"){message success} }";
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("deleteMissionModel"))).getBoolean("success");
    }


  }

  protected static class GetAllMissionModelsId extends GraphRequest {


    public String getRequest() {
      return "query { adaptations {  id }}";
    }

    final ArrayList<String> idsL = new ArrayList<>();

    public ArrayList<String> getMissionModelIds() {
      return idsL;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      JSONArray ids = ((JSONObject) response.get("data")).getJSONArray("adaptations");
      for (int i = 0; i < ids.length(); i++) {
        idsL.add(((JSONObject) ids.get(i)).getString("id"));
      }
      return true;
    }
  }


  protected static class SessionAliveRequest extends GraphRequest {

    public String getRequest() {
      return "query { session { message success }}";
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("login"))).getString("success").equals("true");
    }
  }


  protected class DeleteActivityInstanceRequest extends GraphRequest {

    final ActivityInstance act;
    final Plan plan;

    public DeleteActivityInstanceRequest(ActivityInstance act, Plan plan) {
      this.act = act;
      this.plan = plan;
    }

    public String getRequest() {
      return "mutation { deleteActivityInstance(planId: \""
             + getPlanId(plan)
             + "\", activityInstanceId:\""
             + getActivityInstanceId(act)
             + "\"){ message success }}";
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return true;
    }
  }

  protected class DeletePlanRequest extends GraphRequest {

    Plan plan;
    Long id;

    public DeletePlanRequest(Plan plan) {
      this.plan = plan;
    }

    public DeletePlanRequest(Long id) {
      this.id = id;
    }

    @Override
    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { delete_plan_by_pk(id: \"");
      if (plan != null) {
        sbPlanRequest.append(getPlanId(plan));
      } else {
        sbPlanRequest.append(id);
      }
      sbPlanRequest.append("\"){ id }}");
      return sbPlanRequest.toString();
    }


    @Override
    public boolean handleResponse(JSONObject response) {
      Long id = ((JSONObject) (((JSONObject) response.get("data")).get("delete_plan_by_pk"))).getLong("id");
      if (planIds.containsValue(id)) {
        planIds.values().remove(id);
      }
      return true;
    }
  }

  protected class CreatePlanRequest extends GraphRequest {
    final Time horizonBegin;
    final  Duration horizonEnd;
    final Plan plan;
    final Map<String, AerieStateCache> cache;

    public CreatePlanRequest(Plan plan, Duration horizonBegin, Duration duration, Map<String, AerieStateCache> cache) {
      this.horizonBegin = planningHorizon.getStartHuginn();
      this.horizonEnd = duration;
      this.plan = plan;
      this.cache = cache;
    }

    public String getRequest() {
      return "mutation { insert_plan_one(object : { model_id: "
             + "" + AMMOS_MISSION_MODEL_ID
             + ", start_time:\""
             + horizonBegin
             + "\", duration:\""
             + horizonEnd.in(Duration.SECONDS) + "s"
             + "\", name:\""
             + createPlanName()
             + "\"}){ id }}";
    }

    public boolean handleResponse(JSONObject response) {
      Long id = ((JSONObject) (((JSONObject) response.get("data")).get("insert_plan_one"))).getLong("id");
      addPlanId(plan, id);
      stateCaches.put(plan, this.cache);
      return true;
    }


  }


  protected class CreateActivityInstanceRequest extends GraphRequest {
    final ActivityInstance instance;
    final Plan plan;

    public CreateActivityInstanceRequest(ActivityInstance instance, Plan plan) {
      this.instance = instance;
      this.plan = plan;
    }


    public String getGraphlqlVersionOfParameter(SerializedValue param) {
      return JsonEncoding.encode(param).toString();
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();

      Long planid = getPlanId(plan);

      if (!DEBUG && planid == null) {
        throw new RuntimeException("plan request has to be sent before adding activity instances");
      }

      boolean atLeastOne = false;
      sbPlanRequest.append("mutation { insert_activity_one ( object: {");

      StringBuilder sbParams = new StringBuilder();
      sbParams.append("arguments :{ ");


      if (instance.getDuration() != null) {
        sbParams.append("duration :").append(instance
                                                 .getDuration()
                                                 .in(Duration.MICROSECOND)).append(",");
        atLeastOne = true;
      }
      for (Map.Entry<String, SerializedValue> entry : instance.getArguments().entrySet()) {
        atLeastOne = true;
        String fakeParamName = entry.getKey();
        sbParams.append(fakeParamName).append(":").append(getGraphlqlVersionOfParameter(entry.getValue())).append(",");

      }

      sbParams.append("},");
      if (atLeastOne) {
        sbPlanRequest.append(sbParams);
      }
      sbPlanRequest.append("start_offset:\"");
      sbPlanRequest.append(instance.getStartTime());
      sbPlanRequest.append("\", type:\"");
      sbPlanRequest.append(instance.getType().getName());
      sbPlanRequest.append("\", plan_id:");
      sbPlanRequest.append(planid);
      sbPlanRequest.append("}){ id }}");

      return sbPlanRequest.toString();

    }

    public boolean handleResponse(JSONObject response) {
      Long id =
          ((JSONObject) (((JSONObject) response.get("data")).get("insert_activity_one"))).getLong(
              "id");
      addActInstanceId(instance, id);
      return true;
    }

  }

  public boolean createSimulation(Plan plan) {
    CreateSimulation req = new CreateSimulation(plan);
    return postRequest(req);
  }

  protected class CreateSimulation extends GraphRequest {

    private Long id;

    public CreateSimulation(Plan plan) {
      Long id = planIds.get(plan);
      if (id == null) {

      } else {
        this.id = id;
      }
    }


    @Override
    public String getRequest() {
      return String.format("mutation {insert_simulation_one(object: {plan_id: %d, arguments: {}}) {id }}", this.id);

    }

    @Override
    public boolean handleResponse(final JSONObject response) {
      return true;
    }
  }

  protected class SimulatePlanRequest extends GraphRequest {

    private final Plan plan;
    private Map<String, AerieStateCache> valueCache;

    protected SimulatePlanRequest(Plan plan)
    {
      this.plan = plan;
    }

    public String getRequest() {
      return "query { simulate(planId :"
             + getPlanId(plan)
             + "){status}";
    }

    public boolean handleResponse(JSONObject response) {
      JSONArray id = ((JSONObject) (((JSONObject) response.get("data")).get("simulate"))).getJSONArray("results");

      for (int i = 0; i < id.length(); i++) {
        JSONObject stateNameValues = id.getJSONObject(i);
        String name = stateNameValues.getString("name");

        AerieStateCache map;
        map = valueCache.get(name);

        if (map == null) {

          if (Double.class.equals(stateTypes.get(plan).get(name))) {
            valueCache.put(name, new AerieStateCache(name, Double.class));
          } else if (Integer.class.equals(stateTypes.get(plan).get(name))) {
            valueCache.put(name, new AerieStateCache(name, Integer.class));
          } else if (String.class.equals(stateTypes.get(plan).get(name))) {
            valueCache.put(name, new AerieStateCache(name, String.class));
          }

          valueCache.put(name, new AerieStateCache(name, Double.class));
          map = valueCache.get(name);
        }

        Duration startSim = fromAerieTime(stateNameValues.getString("start"));
        JSONArray values = stateNameValues.getJSONArray("values");
        for (int j = 0; j < values.length(); j++) {
          JSONObject xy = values.getJSONObject(j);
          Duration elapsed = Duration.of((xy.getLong("x") / 1000000), Duration.SECONDS);
          Object value = xy.get("y");
          if (Double.class.equals(stateTypes.get(plan).get(name))) {
            value = xy.getDouble("y");
          } else if (Integer.class.equals(stateTypes.get(plan).get(name))) {
            value = xy.getInt("y");
          } else if (String.class.equals(stateTypes.get(plan).get(name))) {
            value = xy.getString("y");
          }
          map.setValue(startSim.plus(elapsed), value);
        }
      }

      return true;
    }


  }

  protected class UpdateInstanceRequest extends GraphRequest {

    final Plan plan;
    final ActivityInstance act;

    protected UpdateInstanceRequest(ActivityInstance act, Plan plan) {
      this.plan = plan;
      this.act = act;
    }


    public String getRequest() {
      return "mutation { updateActivityInstance(activityInstance : { id :\""
             + getActivityInstanceId(act)
             + "\", parameters:{name:\"\"}"
             + ", startTimestamp:\""
             + act.getStartTime()
             + "\", type:\""
             + act.getType().getName()
             + "\"}, planId :\""
             + getPlanId(plan)
             + "\"){ message success }}";
    }

    public boolean handleResponse(JSONObject response) {
      return true;
    }

  }

  public boolean getCredentialsFromUser() {

    JTextField field1 = new JTextField();
    JPasswordField field2 = new JPasswordField();
    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("JPL username"));
    panel.add(field1);
    panel.add(new JLabel("Password"));
    panel.add(field2);
    int result = JOptionPane.showConfirmDialog(null, panel, "Login to AERIE",
                                               JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      this.username = field1.getText();
      this.password = String.valueOf(field2.getPassword());
      return true;

    } else {
      return false;
    }
  }


}
