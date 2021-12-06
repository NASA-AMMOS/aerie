package gov.nasa.jpl.aerie.scheduler;


import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 *
 */
public class AerieController {

  private static final boolean DEBUG = true;
  private static final String IT_PLAN_BASENAME = "Plan_";
  private static final java.time.Duration TIMEOUT_HTTP = java.time.Duration.ofMinutes(10);

  //TODO: populate these with answers from AERIE
  private final Map<Plan, Long> planIds;
  private final Map<Plan, Map<String, AerieStateCache>> stateCaches;
  private final Map<Plan, Duration> planStartTimes;
  private final MissionModelWrapper missionModelWrapper;


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
  private final boolean isSessionAlive = true;


  public AerieController(final String distantAerieURL,
                         final int missionModelId,
                         final boolean authenticationRequired,
                         final PlanningHorizon planningHorizon,
                         final MissionModelWrapper missionModel) {
    this(distantAerieURL, missionModelId,planningHorizon,missionModel);
    this.authenticationRequired = authenticationRequired;
  }

  public AerieController(final String distantAerieURL,
                         final int missionModelId,
                         final PlanningHorizon planningHorizon,
                         final MissionModelWrapper missionModel) {
    this(distantAerieURL, missionModelId, "", planningHorizon,missionModel);
  }

  public AerieController(final String distantAerieURL,
                         final int missionModelId,
                         final String planPrefix,
                         final PlanningHorizon planningHorizon,
                         final MissionModelWrapper missionModel) {
    this.missionModelWrapper = missionModel;
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
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
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

  public Plan fetchPlan(final long planId){
    final var req = new FetchPlanRequest(planId);
    postRequest(req);
    return req.getPlan();
  }

  protected boolean isSessionAlive() {
    SessionAliveRequest sar = new SessionAliveRequest();
    postRequest(sar);
    return this.isSessionAlive;
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
    if(!currentlyTryingToConnect) {
      authenticateIfNecessary();
    }
    String jsonString = request.getRequest();
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("query", jsonString);

    System.out.println(jsonString);

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
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println(response.statusCode());
    if (response.statusCode() != 200) {
      return false;
    } else {
      try {
        GZIPInputStream body = new GZIPInputStream(response.body());
        Reader reader = new InputStreamReader(body, "UTF-8");
        Writer writer = new StringWriter();
          char[] buffer = new char[10240];
          int length = 0;
          while ((length = reader.read(buffer)) > 0) {
            writer.write(buffer, 0, length);
          }
          String bodystr = writer.toString();
          System.out.println(bodystr);
          JSONObject json = new JSONObject(bodystr);
          return request.handleResponse(json);
      } catch (ZipException e) {
        //probably not in GZIP format
        e.printStackTrace();
        if(response!=null) {
          System.out.println(response.body());
        }
        return false;
      } catch (IOException e) {
        e.printStackTrace();
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
      System.out.println("Plan has never been sent to Aerie");
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
      System.out.println("Plan or act have never been sent to Aerie");
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
      System.out.println("Plan have never been sent to Aerie");
    } else {
      var simulationValueCache = stateCaches.get(plan);
      SimulatePlanRequest spr = new SimulatePlanRequest(plan);
      ret = postRequest(spr);
    }
    return ret;
  }

  public boolean deleteActivity(ActivityInstance act, Plan plan) {
    boolean ret = true;
    if (planIds.get(plan) == null || activityInstancesIds.get(act) == null) {
      ret = false;
      System.out.println("Plan or act have never been sent to Aerie");
    } else {
      DeleteActivityInstanceRequest deleteActivityInstanceRequest = new DeleteActivityInstanceRequest(act, plan);
      ret = postRequest(deleteActivityInstanceRequest);
    }
    return ret;
  }

  public boolean initEmptyPlan(Plan plan, Duration horizonBegin, Duration horizonEnd, Map<String, AerieStateCache> cache) {
    boolean ret = true;

    CreatePlanRequest planRequest = new CreatePlanRequest(plan, horizonBegin, horizonEnd, cache);
    boolean result = postRequest(planRequest);
    if (!result) {
      ret = false;
    }

    //StateTypesRequest str = new StateTypesRequest(plan);
    //ret = postRequest(str);
    planStartTimes.put(plan, horizonBegin);
    return ret;
  }


  public boolean sendPlan(Plan plan, Duration horizonBegin, Duration horizonEnd, Map<String, AerieStateCache> cache) {
    boolean ret = true;
    var planId = planIds.get(plan);
    boolean result = true;
    if(planId == null) {
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
          //delete all activities and plan
          //  DeletePlanRequest delPlanReq = new DeletePlanRequest(plan);
          //   postRequest(delPlanReq);
          break;
        }
      }
    }

    //StateTypesRequest str = new StateTypesRequest(plan);
    //ret = postRequest(str);
    planStartTimes.put(plan, horizonBegin);

    return ret;

  }

  public boolean sendActivityInstance(Plan plan, ActivityInstance act){
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


        System.out.println("Required state with name " + nameState + " has been found in the definition");
      }

    }

    return null;
  }

  public Object getStringValue(Plan plan, String nameState, Duration t) {
    return "";
  }

  public Object getIntegerValue(Plan plan, String nameState, Duration t) {
    return 0;
  }

  protected abstract class GraphRequest {

    public abstract String getRequest();

    public abstract boolean handleResponse(JSONObject response);


  }

  protected class FetchPlanRequest extends GraphRequest {

    public String getRequest() {
      var req = "query { plan_by_pk(id:%d) { activities { start_offset type arguments } duration start_time }} ";
      return String.format(req, id);
    }

    public FetchPlanRequest(long id){
      this.id = id;
    }

    Plan plan;
    long id;

    public Plan getPlan() {
      return plan;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      var jsonplan = ((JSONObject) response.get("data")).getJSONObject("plan_by_pk");
      var activities = jsonplan.getJSONArray("activities");
      this.plan = new PlanInMemory(missionModelWrapper);
      for (int i = 0; i < activities.length(); i++){
        var actInst = jsonToInstance(activities.getJSONObject(i));
        this.plan.add(actInst);
      }
      return true;
    }
  }

  private Parameter getParamName(List<Parameter> params, String name){
    for(var param:params){
      if(param.name().equals(name)){
        return param;
      }
    }
    return null;
  }

  private ActivityInstance jsonToInstance(JSONObject jsonActivity){
    String type = jsonActivity.getString("type");
    var actTypes = missionModelWrapper.getMissionModel().getTaskSpecificationTypes();
    var specType = actTypes.get(type);
    if(specType == null){
      throw new IllegalArgumentException("Activity type is not present in mission model");
    }
    var schedulerActType = missionModelWrapper.getActivityType(type);
    if(schedulerActType == null){
      throw new IllegalArgumentException("Activity type is not present in scheduler mission model wrapper");
    }

    ActivityInstance act = new ActivityInstance("fetched_" + java.util.UUID.randomUUID(), schedulerActType);
    String start = jsonActivity.getString("start_offset");
    var params = jsonActivity.getJSONObject("arguments");
    for(var paramName : params.keySet()){
      var visitor = new DemuxJson(paramName, params);
      var paramSpec = getParamName(specType.getParameters(),paramName);
      var valueParam = paramSpec.schema().match(visitor);
      act.addParameter(paramName, valueParam);
    }
    act.setStartTime(DemuxJson.fromString(start));
    var actDurationAsParam =act.getParameters().get("duration");
    if(actDurationAsParam!= null){
      act.setDuration((Duration)actDurationAsParam);
      act.getParameters().remove("duration");
    } else{
      throw new IllegalArgumentException("Parameters with name \"duration\" has not been found, cannot set activity instance duration");
    }

    return act;
  }

  protected class PlanIdRequest extends GraphRequest {

    public String getRequest() {
      return "query { plan { id} } ";
    }

    ArrayList<Long> idsL = new ArrayList<>();

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
    Plan plan;

    public StateTypesRequest(Plan plan) {
      this.plan = plan;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query ResourceTypes { stateTypes( missionModelId: \"");
      sbPlanRequest.append(AMMOS_MISSION_MODEL_ID);
      sbPlanRequest.append("\"){ name schema }}");
      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {

      var map = stateTypes.get(plan);
      if (map == null) {
        map = new HashMap<String, Class<?>>();
        stateTypes.put(plan, map);
      }


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


      StringBuilder sbPlanRequest = new StringBuilder();
      /*
      sbPlanRequest.append("mutation { login(username: \"");
      sbPlanRequest.append(username);
      sbPlanRequest.append("\", password:\"");
      sbPlanRequest.append(password);
      sbPlanRequest.append("\"){ message ssoCookieName ssoCookieValue success }}");*/
      sbPlanRequest.append("{\", username:\"");
      sbPlanRequest.append(username);
      sbPlanRequest.append("\", password:\"}");
      sbPlanRequest.append(password);

      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      authorizationHeader =
          ((JSONObject) (((JSONObject) response.get("data")).get("login"))).getString("ssoCookieValue");
      return true;
    }
  }

  protected class LogoutRequest extends GraphRequest {

    public String getRequest() {


      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { logout { message success }}");
      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return true;
    }
  }

  protected class DeleteMissionModel extends GraphRequest {

    String delete;

    public DeleteMissionModel(String id) {
      delete = id;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { deleteMissionModel(  id :\"");
      sbPlanRequest.append(delete);
      sbPlanRequest.append("\"){message success} }");
      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("deleteMissionModel"))).getBoolean("success");
    }


  }

  protected class GetAllMissionModelsId extends GraphRequest {


    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query { adaptations {  id }}");
      return sbPlanRequest.toString();
    }

    ArrayList<String> idsL = new ArrayList<String>();

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


  protected class SessionAliveRequest extends GraphRequest {

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query { session { message success }}");
      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("login"))).getString("success").equals("true");
    }
  }


  protected class DeleteActivityInstanceRequest extends GraphRequest {

    ActivityInstance act;
    Plan plan;

    public DeleteActivityInstanceRequest(ActivityInstance act, Plan plan) {
      this.act = act;
      this.plan = plan;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { deleteActivityInstance(planId: \"");
      sbPlanRequest.append(getPlanId(plan));
      sbPlanRequest.append("\", activityInstanceId:\"");
      sbPlanRequest.append(getActivityInstanceId(act));
      sbPlanRequest.append("\"){ message success }}");
      return sbPlanRequest.toString();
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
      int id = ((JSONObject) (((JSONObject) response.get("data")).get("delete_plan_by_pk"))).getInt("id");
      if(planIds.containsValue(id)){
        planIds.values().remove(id);
      }
      return true;
    }
  }

  protected class CreatePlanRequest extends GraphRequest {
    Time horizonBegin;
    Duration horizonEnd;
    Plan plan;
    Map<String, AerieStateCache> cache;

    public CreatePlanRequest(Plan plan, Duration horizonBegin, Duration duration, Map<String, AerieStateCache> cache) {
      this.horizonBegin = planningHorizon.getStartHuginn();
      this.horizonEnd = duration;
      this.plan = plan;
      this.cache = cache;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { insert_plan_one(object : { model_id: ");
      sbPlanRequest.append(""+AMMOS_MISSION_MODEL_ID);
      sbPlanRequest.append(", start_time:\"");
      sbPlanRequest.append(horizonBegin);
      sbPlanRequest.append("\", duration:\"");
      sbPlanRequest.append(horizonEnd.in(Duration.SECONDS)+"s");
      sbPlanRequest.append("\", name:\"");
      sbPlanRequest.append(createPlanName());
      sbPlanRequest.append("\"}){ id }}");
      return sbPlanRequest.toString();
    }

    public boolean handleResponse(JSONObject response) {
      Long id = ((JSONObject) (((JSONObject) response.get("data")).get("insert_plan_one"))).getLong("id");
      addPlanId(plan, id);
      stateCaches.put(plan, this.cache);
      return true;
    }


  }


  protected class CreateActivityInstanceRequest extends GraphRequest {
    ActivityInstance instance;
    Plan plan;

    public CreateActivityInstanceRequest(ActivityInstance instance, Plan plan) {
      this.instance = instance;
      this.plan = plan;
    }


    public String getGraphlqlVersionOfParameter(Object param) {
      if (param instanceof Integer) {
        return ((Integer) param).toString();
      } else if (param instanceof Double) {
        return ((Double) param).toString();
      } else if (param instanceof Boolean) {
        return ((Boolean) param).toString();
      } else if (param instanceof Enum) {
        return "\"" + ((Enum) param).toString() + "\"";
      } else if (param instanceof Time) {
        return "\"" + ((Time) param).toString() + "\"";
      } else if (param instanceof StateQueryParam) {
        StateQueryParam sqParam = (StateQueryParam) param;
        gov.nasa.jpl.aerie.constraints.time.Window time = sqParam.timeExpr.computeTime(
            plan,
            gov.nasa.jpl.aerie.constraints.time.Window.between(
                instance.getStartTime(),
                instance.getEndTime()));
        assert (time.isSingleton());
        return getGraphlqlVersionOfParameter(sqParam.state.getValueAtTime(time.start));
      } else if(param instanceof String s) {
        return "\""+ s +"\"";
      }else {
        throw new RuntimeException("Unsupported parameter type");
      }
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();

      Long planid = getPlanId(plan);

      if (DEBUG != true && planid == null) {
        throw new RuntimeException("plan request has to be sent before adding activity instances");
      }

      boolean atLeastOne = false;
      sbPlanRequest.append("mutation { insert_activity_one ( object: {");

      StringBuilder sbParams = new StringBuilder();
      sbParams.append("arguments :{ ");


      if (instance.getDuration() != null) {
        sbParams.append("duration :" + ((int) instance.getDuration().in(gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS)) + ",");
        atLeastOne = true;
      }
      for (Map.Entry<String, Object> entry : instance.getParameters().entrySet()) {
        atLeastOne = true;
        String fakeParamName = entry.getKey();
        sbParams.append(fakeParamName
                        +":"
                        + getGraphlqlVersionOfParameter(entry.getValue())
                        + ",");

      }

      sbParams.append("},");
      if (atLeastOne) {
        sbPlanRequest.append(sbParams);
      }
      sbPlanRequest.append("start_offset:\"");
      sbPlanRequest.append(instance.getStartTime());
      sbPlanRequest.append("\", type:\"");
      sbPlanRequest.append(instance.getType().name);
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

  public boolean createSimulation(Plan plan){
    CreateSimulation req = new CreateSimulation(plan);
    return postRequest(req);
  }

  protected class CreateSimulation extends GraphRequest{

    private Long id;

    public CreateSimulation(Plan plan){
      Long id = planIds.get(plan);
      if(id== null){

      } else{
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
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query { simulate(planId :");
      sbPlanRequest.append(getPlanId(plan));
      sbPlanRequest.append("){status}");
      return sbPlanRequest.toString();
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
          Duration elapsed = Duration.of((xy.getLong("x") / 1000000), Duration.SECONDS );
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

    Plan plan;
    ActivityInstance act;

    protected UpdateInstanceRequest(ActivityInstance act, Plan plan) {
      this.plan = plan;
      this.act = act;
    }


    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { updateActivityInstance(activityInstance : { id :\"");
      sbPlanRequest.append(getActivityInstanceId(act));
      sbPlanRequest.append("\", parameters:{name:\"\"}");
      sbPlanRequest.append(", startTimestamp:\"");
      sbPlanRequest.append(act.getStartTime());
      sbPlanRequest.append("\", type:\"");
      sbPlanRequest.append(act.getType().getName());
      sbPlanRequest.append("\"}, planId :\"");
      sbPlanRequest.append(getPlanId(plan));
      sbPlanRequest.append("\"){ message success }}");
      return sbPlanRequest.toString();
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
