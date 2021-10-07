package gov.nasa.jpl.aerie.scheduler;


import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AerieController {

  private static final boolean DEBUG = true;
  private static final String IT_PLAN_BASENAME = "Plan_";
  private static final java.time.Duration TIMEOUT_HTTP = java.time.Duration.ofMinutes(10);

  //TODO: populate these with answers from AERIE
  private final Map<Plan, String> planIds;
  private final Map<Plan, Map<String, AerieStateCache>> stateCaches;
  private final Map<Plan, Time> planStartTimes;


  private Map<Plan, Map<String, Class<?>>> stateTypes;

  private final Map<ActivityInstance, String> activityInstancesIds;
  private final String AMMOS_ADAPTATION_ID;
  private final String distantAerieURL;
  private final String plan_prefix;

  //AUTHENTICATION
  private boolean authenticationRequired = false;
  private String username = null;
  private String password = null;
  private String authorizationHeader = null;
  private boolean isSessionAlive = true;


  public AerieController(String distantAerieURL, String adaptationId, boolean authenticationRequired) {
    this(distantAerieURL, adaptationId);
    this.authenticationRequired = authenticationRequired;
  }

  public AerieController(String distantAerieURL, String adaptationId) {
    this(distantAerieURL, adaptationId, "");
  }

  public AerieController(String distantAerieURL, String adaptationId, String planPrefix) {
    this.distantAerieURL = distantAerieURL;
    this.AMMOS_ADAPTATION_ID = adaptationId;
    planIds = new HashMap<Plan, String>();
    stateCaches = new HashMap<Plan, Map<String, AerieStateCache>>();
    stateTypes = new HashMap<Plan, Map<String, Class<?>>>();
    activityInstancesIds = new HashMap<ActivityInstance, String>();
    planStartTimes = new HashMap<Plan, Time>();
    plan_prefix = planPrefix;
  }


  public Time fromAerieTime(String aerieTime) {
    return Time.fromString(aerieTime.substring(0, aerieTime.length() - 3));
  }

  private String getActivityInstanceId(ActivityInstance act) {
    return activityInstancesIds.get(act);
  }

  private String getPlanId(Plan plan) {
    return planIds.get(plan);
  }

  private String createPlanName() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
    LocalDateTime localDate = LocalDateTime.now();
    String todayDate = dtf.format(localDate);
    return plan_prefix + IT_PLAN_BASENAME + todayDate;

  }

  public void deleteAllAdaptationsBut(String notThisOne) {
    GetAllAdaptationsId getReq = new GetAllAdaptationsId();
    postRequest(getReq);
    List<String> ids = getReq.getAdaptationIds();
    for (var id : ids) {
      if (!id.equals(notThisOne)) {
        DeleteAdaptation da = new DeleteAdaptation(id);
        postRequest(da);
      }
    }
  }


  protected boolean isSessionAlive() {
    SessionAliveRequest sar = new SessionAliveRequest();
    postRequest(sar);
    return this.isSessionAlive;
  }

  protected void authenticateIfNecessary() {
    if (authenticationRequired) {
      if (!isSessionAlive()) {
        LoginRequest loginRequest = new LoginRequest();
        postRequest(loginRequest);
      }
    }
  }

  protected boolean postRequest(GraphRequest request) {
    authenticateIfNecessary();
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
    HttpResponse<String> response = null;
    try {
      response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println(response.statusCode());
    String body = response.body();
    System.out.println(body);
    if (response.statusCode() != 200) {
      return false;
    } else {
      JSONObject json = new JSONObject(body);
      return request.handleResponse(json);
    }

  }


  public void deleteAllPlans() {
    PlanIdRequest pir = new PlanIdRequest();
    boolean ret = postRequest(pir);
    for (String id : pir.getIds()) {
      DeletePlanRequest dpr = new DeletePlanRequest(id);
      ret = postRequest(dpr);
    }
  }

  protected void addPlanId(Plan plan, String id) {
    this.planIds.put(plan, id);
  }

  protected void addActInstanceId(ActivityInstance act, String id) {
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

  public boolean simulatePlan(Plan plan, Duration timestep) {
    boolean ret = true;
    if (planIds.get(plan) == null) {
      ret = false;
      System.out.println("Plan have never been sent to Aerie");
    } else {
      var simulationValueCache = stateCaches.get(plan);
      SimulatePlanRequest spr = new SimulatePlanRequest(plan, timestep, simulationValueCache);
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

  public boolean initEmptyPlan(Plan plan, Time horizonBegin, Time horizonEnd, Map<String, AerieStateCache> cache) {
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


  public boolean sendPlan(Plan plan, Time horizonBegin, Time horizonEnd, Map<String, AerieStateCache> cache) {
    boolean ret = true;
    CreatePlanRequest planRequest = new CreatePlanRequest(plan, horizonBegin, horizonEnd, cache);
    boolean result = postRequest(planRequest);
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

  public Object getDoubleValue(Plan plan, String nameState, Time t) {
    if (this.planIds.get(plan) != null) {
      var statesCache = this.stateCaches.get(plan);
      var statesTypes = this.stateTypes.get(plan);
      var stateCache = statesCache.get(nameState);

      Time startPlan = planStartTimes.get(plan);

      if (stateCache == null) {
        simulatePlan(plan, t.minus(startPlan));
        stateCache = statesCache.get(nameState);
      }

      if (stateCache != null) {
        var value = stateCache.getValue(t, statesTypes.get(nameState));
        if (value != null) {
          return value;
        } else {
          simulatePlan(plan, t.minus(startPlan));
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

  public Object getStringValue(Plan plan, String nameState, Time t) {
    return "";
  }

  public Object getIntegerValue(Plan plan, String nameState, Time t) {
    return 0;
  }

  protected abstract class GraphRequest {

    public abstract String getRequest();

    public abstract boolean handleResponse(JSONObject response);

    public boolean isASuccess(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("createActivityInstances"))).getBoolean("success");
    }
  }


  protected class PlanIdRequest extends GraphRequest {

    public String getRequest() {
      return "query { plans { id} } ";
    }

    ArrayList<String> idsL = new ArrayList<String>();

    public ArrayList<String> getIds() {
      return idsL;
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      JSONArray ids = ((JSONObject) response.get("data")).getJSONArray("plans");
      for (int i = 0; i < ids.length(); i++) {
        idsL.add(((JSONObject) ids.get(i)).getString("id"));
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
      sbPlanRequest.append("query ResourceTypes { stateTypes( adaptationId: \"");
      sbPlanRequest.append(AMMOS_ADAPTATION_ID);
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
      sbPlanRequest.append("mutation { login(username: \"");
      sbPlanRequest.append(username);
      sbPlanRequest.append("\", password:\"");
      sbPlanRequest.append(password);
      sbPlanRequest.append("\"){ message ssoCookieName ssoCookieValue success }}");
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

  protected class DeleteAdaptation extends GraphRequest {

    String delete;

    public DeleteAdaptation(String id) {
      delete = id;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { deleteAdaptation(  id :\"");
      sbPlanRequest.append(delete);
      sbPlanRequest.append("\"){message success} }");
      return sbPlanRequest.toString();
    }

    @Override
    public boolean handleResponse(JSONObject response) {
      return ((JSONObject) (((JSONObject) response.get("data")).get("deleteAdaptation"))).getBoolean("success");
    }


  }

  protected class GetAllAdaptationsId extends GraphRequest {


    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query { adaptations {  id }}");
      return sbPlanRequest.toString();
    }

    ArrayList<String> idsL = new ArrayList<String>();

    public ArrayList<String> getAdaptationIds() {
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
    String id;

    public DeletePlanRequest(Plan plan) {
      this.plan = plan;
    }

    public DeletePlanRequest(String id) {
      this.id = id;
    }

    @Override
    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { deletePlan(id: \"");
      if (plan != null) {
        sbPlanRequest.append(getPlanId(plan));
      } else {
        sbPlanRequest.append(id);
      }
      sbPlanRequest.append("\"){ message success }}");
      return sbPlanRequest.toString();
    }


    @Override
    public boolean handleResponse(JSONObject response) {
      return true;
    }
  }

  protected class CreatePlanRequest extends GraphRequest {
    Time horizonBegin;
    Time horizonEnd;
    Plan plan;
    Map<String, AerieStateCache> cache;

    public CreatePlanRequest(Plan plan, Time horizonBegin, Time horizonEnd, Map<String, AerieStateCache> cache) {
      this.horizonBegin = horizonBegin;
      this.horizonEnd = horizonEnd;
      this.plan = plan;
      this.cache = cache;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("mutation { createPlan(adaptationId: \"");
      sbPlanRequest.append(AMMOS_ADAPTATION_ID);
      sbPlanRequest.append("\", startTimestamp:\"");
      sbPlanRequest.append(horizonBegin);
      sbPlanRequest.append("\", endTimestamp:\"");
      sbPlanRequest.append(horizonEnd);
      sbPlanRequest.append("\", name:\"");
      sbPlanRequest.append(createPlanName());
      sbPlanRequest.append("\"){ id message success }}");
      return sbPlanRequest.toString();
    }

    public boolean handleResponse(JSONObject response) {
      String id = ((JSONObject) (((JSONObject) response.get("data")).get("createPlan"))).getString("id");
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
        Range<Time> time = sqParam.timeExpr.computeTime(
            plan,
            new Range<Time>(
                instance.getStartTime(),
                instance.getEndTime()));
        assert (time.isSingleton());
        return getGraphlqlVersionOfParameter(sqParam.state.getValueAtTime(time.getMinimum()));
      } else {
        throw new RuntimeException("Unsupported parameter type");
      }
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();

      String planid = getPlanId(plan);

      if (DEBUG != true && planid == null) {
        throw new RuntimeException("plan request has to be sent before adding activity instances");
      }

      boolean atLeastOne = false;
      sbPlanRequest.append("mutation { createActivityInstances ( activityInstances: {");
      //if(instance.getParameters().size()>0) {

      StringBuilder sbParams = new StringBuilder();
      sbParams.append("parameters :[ ");


      if (instance.getDuration() != null && !(instance.getType().getName().equals("SimulateAllFakedStates"))) {
        sbParams.append("{ name : \"duration_sec\", value :" + ((int) instance.getDuration().toSeconds()) + "},");
        atLeastOne = true;
      }
      for (Map.Entry<String, Object> entry : instance.getParameters().entrySet()) {
        atLeastOne = true;
        String fakeParamName = entry.getKey();
        sbParams.append("{ name : \""
                        + fakeParamName
                        + "\", value :"
                        + getGraphlqlVersionOfParameter(entry.getValue())
                        + "},");

      }

      sbParams.append("],");
      if (atLeastOne) {
        sbPlanRequest.append(sbParams);
      } else {
        sbPlanRequest.append("parameters :{name :\"\"}, ");
      }
      sbPlanRequest.append("startTimestamp:\"");
      sbPlanRequest.append(instance.getStartTime());
      sbPlanRequest.append("\", type:\"");
      sbPlanRequest.append(instance.getType().name);
      sbPlanRequest.append("\"}, planId:\"");
      sbPlanRequest.append(planid);
      sbPlanRequest.append("\"){ ids message success }}");

      return sbPlanRequest.toString();

    }

    public boolean handleResponse(JSONObject response) {
      if (isASuccess(response)) {

        String id =
            (String) ((JSONObject) (((JSONObject) response.get("data")).get("createActivityInstances"))).getJSONArray(
                "ids").get(0);
        addActInstanceId(instance, id);
        return true;
      } else {
        return false;
      }
    }

  }

  protected class SimulatePlanRequest extends GraphRequest {

    private final Duration samplingPeriod;
    private final Plan plan;
    private Map<String, AerieStateCache> valueCache;

    protected SimulatePlanRequest(
        Plan plan,
        Duration samplingPeriod,
        Map<String, AerieStateCache> simulationValueCache)
    {
      this.plan = plan;
      this.samplingPeriod = samplingPeriod;
      this.valueCache = simulationValueCache;
    }

    public String getRequest() {
      StringBuilder sbPlanRequest = new StringBuilder();
      sbPlanRequest.append("query { simulate(planId :\"");
      sbPlanRequest.append(getPlanId(plan));
      sbPlanRequest.append("\", samplingPeriod :");
      sbPlanRequest.append(samplingPeriod.toMilliseconds() * 1000);
      sbPlanRequest.append("){ message success results { name start values {x y}}}}");
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

        Time startSim = fromAerieTime(stateNameValues.getString("start"));
        JSONArray values = stateNameValues.getJSONArray("values");
        for (int j = 0; j < values.length(); j++) {
          JSONObject xy = values.getJSONObject(j);
          Duration elapsed = Duration.ofSeconds((xy.getLong("x") / 1000000));
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

    List<String> credentials = new ArrayList<String>();
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

