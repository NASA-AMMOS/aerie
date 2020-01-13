package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JSONMatcher;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.mocks.MockHttpHandler;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MerlinCLIAdaptationTests {
    private String resourcesRoot = "src/test/resources";
    private final String baseURL = "http://localhost:27182/api/adaptations";
    private final String activityPath = "activities";
    private final String parameterPath = "parameters";
    private final MockHttpHandler mockHttpHandler = new MockHttpHandler();

    // Used to intercept System.out and System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Before
    public void setUp() {
        setUpStreams();
    }

    @After
    public void cleanUp() {
        restoreStreams();
    }

    /**
     * Tests that the CLI issues a POST to the correct endpoint
     * when asked to create an adaptation.
     */
    @Test
    public void testAdaptationCreation() throws URISyntaxException, IOException {
        String path = String.format("%s/bananatation.jar", resourcesRoot);
        String name = "testName";
        String version = "1.0a.3";
        String location = "427";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_CREATED, "");
        response.addHeader("location", location);

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "--create-adaptation", path, "name="+name, "version="+version };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(baseURL);
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpPost.METHOD_NAME);
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for a list of adaptations.
     *
     * Checks that the response is printed to standard out
     */
    @Test
    public void testReadAdaptationList() throws IOException, URISyntaxException {
        String body = "[" +
                  "{" +
                    "\"name\": \"Test1\"," +
                    "\"version\": \"1\"" +
                  "}," +
                  "{" +
                    "\"name\": \"Test2\"," +
                    "\"version\": \"1\"," +
                    "\"mission\": \"TEST\"," +
                    "\"owner\": \"tester\"" +
                  "}" +
                "]";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "--list-adaptations" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(baseURL);
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = new String(response.getEntity().getContent().readAllBytes());
        assertThat(output.indexOf('[')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('['));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an adaptation by ID.
     *
     * Checks that the response is printed to standard out
     */
    @Test
    public void testReadAdaptation() throws IOException, URISyntaxException {
        String adaptationId = "0123-4567-89ab-cdef";
        String body = "{" +
                  "\"name\": \"Test\"," +
                  "\"version\": \"0.a\"," +
                  "\"mission\": \"Impossible\"," +
                  "\"owner\": \"Tom Cruise\"" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-a", adaptationId, "--view-adaptation" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, adaptationId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = new String(response.getEntity().getContent().readAllBytes());
        assertThat(output.indexOf('{')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('{'));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an adaptation's activity types
     *
     * Checks that the response is printed to standard out
     */
    @Test
    public void testReadActivityTypes() throws IOException, URISyntaxException {
        String adaptationId = "0123-4567-89ab-cdea";
        String body = "[" +
                  "{" +
                    "\"id\": \"3911-5a5e-f6d6-ea2c\"," +
                    "\"name\": \"PeelBanana\"," +
                    "\"parameters\": [" +
                      "{" +
                        "\"name\": \"peelDirection\"," +
                        "\"type\": \"String\"," +
                        "\"defaultValue\": null," +
                        "\"range\": []" +
                      "}" +
                    "]" +
                  "}, {" +
                    "\"id\": \"3911-5a5e-f6d6-ea2d\"," +
                    "\"name\": \"BiteBanana\"," +
                    "\"parameters\": [" +
                      "{" +
                        "\"name\": \"biteSize\"," +
                        "\"type\": \"Double\"," +
                        "\"defaultValue\": null," +
                        "\"range\": []" +
                      "}, {" +
                        "\"name\": \"angle\"," +
                        "\"type\": \"Integer\"," +
                        "\"defaultValue\": 90," +
                        "\"range\": [0, 360]" +
                      "}" +
                    "]" +
                  "}" +
                "]";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-a", adaptationId, "--activity-types" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s", baseURL, adaptationId, activityPath));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = new String(response.getEntity().getContent().readAllBytes());
        assertThat(output.indexOf('[')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('['));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an activity type from an adaptation
     *
     * Checks that the response is printed to standard out
     */
    @Test
    public void testReadActivityType() throws IOException, URISyntaxException {
        String adaptationId = "0123-4567-89ab-cdea";
        String activityId = "3911-5a5e-f6d6-ea2c";
        String body = "{" +
                  "\"id\": \"" + activityId + "\"," +
                  "\"name\": \"PeelBanana\"," +
                  "\"parameters\": [" +
                    "{" +
                      "\"name\": \"peelDirection\"," +
                      "\"type\": \"String\"," +
                      "\"defaultValue\": null," +
                      "\"range\": []" +
                    "}" +
                  "]" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-a", adaptationId, "--activity-type", activityId };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s/%s", baseURL, adaptationId, activityPath, activityId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = new String(response.getEntity().getContent().readAllBytes());
        assertThat(output.indexOf('{')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('{'));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an activity type's parameters
     *
     * Checks that the response is printed to standard out
     */
    @Test
    public void testReadActivityTypeParameters() throws IOException, URISyntaxException {
        String adaptationId = "0123-4567-89ab-cdea";
        String activityId = "3911-5a5e-f6d6-ea2f";
        String body = "[" +
                  "{" +
                    "\"name\": \"param1\"," +
                    "\"type\": \"String\"," +
                    "\"defaultValue\": \"value1\"," +
                    "\"range\": []" +
                  "}, {" +
                    "\"name\": \"param2\"," +
                    "\"type\": \"Integer\"," +
                    "\"defaultValue\": 30," +
                    "\"range\": [0, 60]" +
                  "}" +
                "]";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-a", adaptationId, "--activity-type-parameters", activityId };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s/%s/%s", baseURL, adaptationId, activityPath, activityId, parameterPath));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = new String(response.getEntity().getContent().readAllBytes());
        assertThat(output.indexOf('[')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('['));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a DELETE to the correct endpoint
     * when asked to delete an adaptation
     */
    @Test
    public void testDeleteAdaptation() throws URISyntaxException {
        String adaptationId = "485a-36a9=bc7e-7fec";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-a", adaptationId, "--delete-adaptation" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, adaptationId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpDelete.METHOD_NAME);
    }
}