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
 */
public class AerieController {

    private static final boolean DEBUG = true;
    private static final String itPlanBasename = "Plan_";

    //TODO: populate these with answers from AERIE
    private final Map<Plan, String> planIds;
    private final Map<ActivityInstance, String> activityInstancesIds;
    private final String AMMOS_ADAPTATION_ID;
    private final String distantAerieURL;


    //AUTHENTICATION
    private boolean authenticationRequired = false;
    private String username = null;
    private String password = null;
    private String authorizationHeader= null;
    private boolean isSessionAlive = true;


    public AerieController(String distantAerieURL, String adaptationId, boolean authenticationRequired){
       this(distantAerieURL,adaptationId);
       this.authenticationRequired = true;
    }

    public AerieController(String distantAerieURL, String adaptationId){
        this.distantAerieURL = distantAerieURL;
        this.AMMOS_ADAPTATION_ID = adaptationId;
        planIds = new HashMap<Plan, String>();
        activityInstancesIds = new HashMap<ActivityInstance, String>();
    }


    private String getActivityInstanceId(ActivityInstance act){
        return activityInstancesIds.get(act);
    }

    private String getPlanId(Plan plan){
        return planIds.get(plan);
    }

    private String createPlanName(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
        LocalDateTime localDate = LocalDateTime.now();
        String todayDate = dtf.format(localDate);
        return itPlanBasename+todayDate;

    }

    public void deleteAllAdaptationsBut(String notThisOne){
        GetAllAdaptationsId getReq =new GetAllAdaptationsId();
        postRequest(getReq);
        List<String> ids = getReq.getAdaptationIds();
        for(var id : ids){
            if(!id.equals(notThisOne)){
                DeleteAdaptation da = new DeleteAdaptation(id);
                postRequest(da);
            }
        }

    }

    protected boolean isSessionAlive(){
        SessionAliveRequest sar = new SessionAliveRequest();
        postRequest(sar);
        return this.isSessionAlive;
    }

    protected void authenticateIfNecessary(){
        if(authenticationRequired){
            if(!isSessionAlive()) {
                LoginRequest loginRequest = new LoginRequest();
                postRequest(loginRequest);
            }
        }
    }

    protected boolean postRequest(GraphRequest request){
        authenticateIfNecessary();
        String jsonString = request.getRequest();
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("query", jsonString);

        System.out.println(jsonString);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(distantAerieURL));

        httpRequestBuilder = httpRequestBuilder.timeout(
                        java.time.Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept", "application/json")
                .header("DNT", "1")
                .header("Origin", distantAerieURL)
                //.header("Content-Type", "application/graphql")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObj.toString()));
        if(authorizationHeader!= null){
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
        if(response.statusCode() != 200){
            return false;
        } else{
            JSONObject json = new JSONObject(body);
            return request.handleResponse(json);
        }

    }


    public void deleteAllPlans(){
        PlanIdRequest pir = new PlanIdRequest();
        boolean ret = postRequest(pir);
        for(String id: pir.getIds()){
            DeletePlanRequest dpr = new DeletePlanRequest(id);
             ret = postRequest(dpr);
        }
    }

    protected void addPlanId(Plan plan, String id){
        this.planIds.put(plan, id);
    }

    protected void addActInstanceId(ActivityInstance act, String id){
        this.activityInstancesIds.put(act, id);
    }


    public boolean deletePlan(Plan plan){
        boolean ret = true;
        if(planIds.get(plan) == null) {
            ret = false;
            System.out.println("Plan has never been sent to Aerie");
        } else{
            DeletePlanRequest deletePlanRequest = new DeletePlanRequest(plan);
            ret = postRequest(deletePlanRequest);
        }
        return ret;
    }


    public boolean updateActivity(ActivityInstance act, Plan plan){
        boolean ret = true;
        if(planIds.get(plan) == null || activityInstancesIds.get(act) == null) {
            ret = false;
            System.out.println("Plan or act have never been sent to Aerie");
        } else{
            UpdateInstanceRequest updateActivityInstanceRequest = new UpdateInstanceRequest(act, plan);
            ret = postRequest(updateActivityInstanceRequest);
        }
        return ret;
    }

    public boolean simulatePlan(Plan plan, Duration timestep){
        boolean ret = true;
        if(planIds.get(plan) == null) {
            ret = false;
            System.out.println("Plan have never been sent to Aerie");
        } else{
            SimulatePlanRequest spr = new SimulatePlanRequest(plan, timestep);
            ret = postRequest(spr);
        }
        return ret;    }

    public boolean deleteActivity(ActivityInstance act, Plan plan){
        boolean ret = true;
        if(planIds.get(plan) == null || activityInstancesIds.get(act) == null) {
            ret = false;
            System.out.println("Plan or act have never been sent to Aerie");
        } else{
            DeleteActivityInstanceRequest deleteActivityInstanceRequest = new DeleteActivityInstanceRequest(act, plan);
            ret = postRequest(deleteActivityInstanceRequest);
        }
        return ret;
    }


    public boolean sendPlan(Plan plan, Time horizonBegin, Time horizonEnd){
        boolean ret = true;
        CreatePlanRequest planRequest = new CreatePlanRequest(plan, horizonBegin, horizonEnd);
        boolean result = postRequest(planRequest);
        if(!result){
            ret = false;
        } else {
            String planId = planIds.get(plan);
            for (ActivityInstance act : plan.getActivitiesByTime()) {
                if(act.getType().getName().equals("Window")) continue;
                CreateActivityInstanceRequest actInstRequest = new CreateActivityInstanceRequest(act, plan);
                boolean res = postRequest(actInstRequest);
                if(!res){
                    ret = false;
                    //delete all activities and plan
                  //  DeletePlanRequest delPlanReq = new DeletePlanRequest(plan);
                 //   postRequest(delPlanReq);
                    break;
                }
            }
        }
        return ret;

    }


    protected abstract class GraphRequest {

        public abstract String getRequest();

        public abstract boolean handleResponse(JSONObject response);

        public boolean isASuccess(JSONObject response){
           return  ((JSONObject) ( ((JSONObject) response.get("data")).get("createActivityInstances"))).getBoolean("success");
        }
    }


    protected class PlanIdRequest extends GraphRequest{

        public  String getRequest(){
            return "query { plans { id} } ";
        }
        ArrayList<String> idsL = new ArrayList<String>();

        public ArrayList<String> getIds(){
            return idsL;
        }

        @Override
        public boolean handleResponse(JSONObject response) {
            JSONArray ids = ((JSONObject) response.get("data")).getJSONArray("plans");
            for (int i=0;i<ids.length();i++){
                idsL.add(((JSONObject)ids.get(i)).getString("id"));
            }
            return true;
        }
    }

    protected class LoginRequest extends GraphRequest {

        public  String getRequest(){
            if(username == null || password == null){
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
            authorizationHeader= ((JSONObject) ( ((JSONObject) response.get("data")).get("login"))).getString("ssoCookieValue");
                return true;
        }
    }

    protected class LogoutRequest extends GraphRequest {

        public  String getRequest(){


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

        public DeleteAdaptation(String id){
            delete=id;
        }

        public  String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();
            sbPlanRequest.append("mutation { deleteAdaptation(  id :\"");
            sbPlanRequest.append(delete);
            sbPlanRequest.append("\"){message success} }");
            return sbPlanRequest.toString();
        }

        @Override
        public boolean handleResponse(JSONObject response) {
            return ((JSONObject) ( ((JSONObject) response.get("data")).get("deleteAdaptation"))).getBoolean("success");
        }


    }

    protected class GetAllAdaptationsId extends GraphRequest {


        public  String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();
            sbPlanRequest.append("query { adaptations {  id }}");
            return sbPlanRequest.toString();
        }

        ArrayList<String> idsL = new ArrayList<String>();

        public ArrayList<String> getAdaptationIds(){
            return idsL;
        }

        @Override
        public boolean handleResponse(JSONObject response) {
            JSONArray ids = ((JSONObject) response.get("data")).getJSONArray("adaptations");
            for (int i=0;i<ids.length();i++){
                idsL.add(((JSONObject)ids.get(i)).getString("id"));
            }
            return true;
        }
    }


        protected class SessionAliveRequest extends GraphRequest {

        public  String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();
            sbPlanRequest.append("query { session { message success }}");
            return sbPlanRequest.toString();
        }

        @Override
        public boolean handleResponse(JSONObject response) {
            return ((JSONObject) ( ((JSONObject) response.get("data")).get("login"))).getString("success").equals("true");
        }
    }



    protected class DeleteActivityInstanceRequest extends GraphRequest {

        ActivityInstance act;
        Plan plan;
        public DeleteActivityInstanceRequest(ActivityInstance act, Plan plan){
            this.act = act;
            this.plan = plan;
        }

        public  String getRequest(){
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

        public DeletePlanRequest(Plan plan){
            this.plan = plan;
        }

        public DeletePlanRequest(String id){
            this.id = id;
        }

        @Override
        public String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();
            sbPlanRequest.append("mutation { deletePlan(id: \"");
            if(plan != null){
                sbPlanRequest.append(getPlanId(plan));
            } else{
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

        public CreatePlanRequest(Plan plan, Time horizonBegin, Time horizonEnd){
            this.horizonBegin = horizonBegin;
            this.horizonEnd = horizonEnd;
            this.plan = plan;
        }

        public String getRequest(){
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

        public boolean handleResponse(JSONObject response){
            String id = ((JSONObject) ( ((JSONObject) response.get("data")).get("createPlan"))).getString("id");
            addPlanId(plan, id);
            return true;
        }


    }


    protected class CreateActivityInstanceRequest extends GraphRequest {
        ActivityInstance instance;
        Plan plan;
        public CreateActivityInstanceRequest(ActivityInstance instance, Plan plan){
            this.instance = instance;
            this.plan = plan;
        }


        public String getGraphlqlVersionOfParameter(Object param){
            if(param instanceof Integer){
                return ((Integer)param).toString();
            } else if(param instanceof Double){
                return ((Double)param).toString();
            }else if(param instanceof Boolean){
                return ((Boolean)param).toString();
            }
            else if(param instanceof Enum){
                return "\""+((Enum)param).toString()+"\"";
            }
            else if(param instanceof Time){
                return "\""+((Time) param).toString()+"\"";
            }
            else if(param instanceof StateQueryParam){
                StateQueryParam sqParam = (StateQueryParam) param;
                Range<Time> time = sqParam.timeExpr.computeTime(plan,new Range<Time>(instance.getStartTime(), instance.getEndTime()) );
                assert(time.isSingleton());
                return getGraphlqlVersionOfParameter(sqParam.state.getValueAtTime(time.getMinimum()));
            }
            else{
                throw new RuntimeException("Unsupported parameter type");
            }
        }

        public String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();

            String planid = getPlanId(plan);

            if(DEBUG != true && planid == null){
                throw new RuntimeException("plan request has to be sent before adding activity instances");
            }

            sbPlanRequest.append("mutation { createActivityInstances ( activityInstances: {");
            if(instance.getParameters().size()>0) {
                sbPlanRequest.append("parameters :[ ");

                for(Map.Entry<String, Object>  entry: instance.getParameters().entrySet()) {

                    String fakeParamName = entry.getKey();
                    sbPlanRequest.append("{ name : \""+fakeParamName+"\", value :"+getGraphlqlVersionOfParameter(entry.getValue())+"},");

                }
                if(instance.getDuration()!= null) {
                    sbPlanRequest.append("{ name : \"duration_sec\", value :" + ((int) instance.getDuration().toSeconds()) + "},");
                }
                sbPlanRequest.append("],");
            } else{
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

        public boolean handleResponse(JSONObject response){
            if(isASuccess(response)){

                String id = (String) ((JSONObject) ( ((JSONObject) response.get("data")).get("createActivityInstances"))).getJSONArray("ids").get(0);
                addActInstanceId(instance, id);
                return true;
            }
            else{
                return false;
            }
        }

    }

    protected class SimulatePlanRequest extends GraphRequest {

        private final Duration samplingPeriod;
        private final Plan plan;

        protected SimulatePlanRequest(Plan plan, Duration samplingPeriod){
            this.plan = plan;
            this.samplingPeriod = samplingPeriod;
        }

        public String getRequest(){
            StringBuilder sbPlanRequest = new StringBuilder();
            sbPlanRequest.append("query { simulate(planId :\"");
            sbPlanRequest.append(getPlanId(plan));
            sbPlanRequest.append("\", samplingPeriod :");
            sbPlanRequest.append(samplingPeriod.toMilliseconds()*1000);
            sbPlanRequest.append("){ message success activities results { name start}}}");
            return sbPlanRequest.toString();
        }

        public boolean handleResponse(JSONObject response){
            return true;
        }


    }

    protected class UpdateInstanceRequest extends GraphRequest {

        Plan plan;
        ActivityInstance act;

        protected  UpdateInstanceRequest(ActivityInstance act, Plan plan) {
            this.plan = plan;
            this.act = act;
        }


            public String getRequest(){
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

        public boolean handleResponse(JSONObject response){
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

