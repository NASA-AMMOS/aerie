package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.writeJson;

public class RemotePlanRepository implements PlanRepository {

    private final String baseURL = "http://localhost:27183/plans";
    private final String instancePath = "activities";
    private HttpHandler httpClient;

    public RemotePlanRepository(HttpHandler httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String createPlan(String planJson) throws PlanCreateFailureException {
        HttpPost request = new HttpPost(baseURL);

        try {
            addJsonToRequest(request, planJson);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == HttpStatus.SC_CREATED) {
            if (response.containsHeader("location")) {
                String id = response.getFirstHeader("location").toString();
                return id;
            } else {
                throw new ApiContractViolationException("Plan created but location header not found.");
            }
        } else {
            // TODO: The message will be JSON, should be parsed and the actual reason extracted
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch (IOException e) {
                throw new Error(e);
            }
            throw new PlanCreateFailureException(message);
        }
    }

    @Override
    public void updatePlan(String planId, String planUpdateJson) throws PlanUpdateFailureException {
        HttpPatch request = new HttpPatch(String.format("%s/%s", baseURL, planId));

        try {
            addJsonToRequest(request, planUpdateJson);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch(IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == HttpStatus.SC_NOT_FOUND) {
            throw new PlanUpdateFailureException(String.format("Plan with id %s not found.", planId));
        }
        else if (status != HttpStatus.SC_NO_CONTENT && status != HttpStatus.SC_OK) {
            // TODO: The message will be JSON, should be parsed and the actual reason extracted
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch (IOException e) {
                throw new Error(e);
            }
            throw new PlanUpdateFailureException(message);
        }
    }

    @Override
    public void deletePlan(String planId) throws PlanDeleteFailureException {
        HttpDelete request = new HttpDelete(String.format("%s/%s", baseURL, planId));

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch(IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_OK) {
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new PlanDeleteFailureException(message);
        }
    }

    @Override
    public void downloadPlan(String planId, String outName) throws PlanDownloadFailureException {
        HttpGet request = new HttpGet(String.format("%s/%s", baseURL, planId));

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == HttpStatus.SC_OK) {
            try {
                String responseString = new String(response.getEntity().getContent().readAllBytes());
                writeJson(responseString, Path.of(outName));
            } catch(IOException e) {
                throw new Error(e);
            }

        } else {
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new PlanDownloadFailureException(message);
        }
    }

    @Override
    public String getPlanList() throws GetPlanListFailureException {
        HttpGet request = new HttpGet(baseURL);

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == HttpStatus.SC_OK) {
            try {
                return new String(response.getEntity().getContent().readAllBytes());
            } catch (IOException e) {
                throw new Error(e);
            }
        } else {
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new GetPlanListFailureException(message);
        }
    }

    @Override
    public void appendActivityInstances(String planId, String instanceListJson) throws AppendActivityInstancesFailureException {
        HttpPost request = new HttpPost(String.format("%s/%s", baseURL, planId));

        try {
            addJsonToRequest(request, instanceListJson);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_CREATED) {
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new AppendActivityInstancesFailureException(message);
        }
    }

    @Override
    public String getActivityInstance(String planId, String activityId) throws GetActivityInstanceFailureException {
        HttpGet request = new HttpGet(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == HttpStatus.SC_OK) {
            try {
                return new String(response.getEntity().getContent().readAllBytes());
            } catch (IOException e) {
                throw new Error(e);
            }

        } else {
            String message;
            try {
                message = prettify(new String(response.getEntity().getContent().readAllBytes()));
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new GetActivityInstanceFailureException(message);
        }
    }

    @Override
    public void updateActivityInstance(String planId, String activityId, String activityInstanceJson) throws UpdateActivityInstanceFailureException {
        HttpPatch request = new HttpPatch(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));

        try {
            addJsonToRequest(request, activityInstanceJson);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch(IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_NO_CONTENT) {
            String message;
            try {
                message = prettify(new String(response.getEntity().getContent().readAllBytes()));
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new UpdateActivityInstanceFailureException(message);
        }
    }

    @Override
    public void deleteActivityInstance(String planId, String activityId) throws DeleteActivityInstanceFailureException {
        HttpDelete request = new HttpDelete(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch(IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_OK) {
            String message;
            try {
                message = prettify(new String(response.getEntity().getContent().readAllBytes()));
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new DeleteActivityInstanceFailureException(message);
        }
    }

    private void addJsonToRequest(HttpEntityEnclosingRequest request, String json) throws UnsupportedEncodingException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(json));
    }
}
