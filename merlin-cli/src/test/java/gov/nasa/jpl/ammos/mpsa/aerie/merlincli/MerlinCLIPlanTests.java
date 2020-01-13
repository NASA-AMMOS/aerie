package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JSONMatcher;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.mocks.MockHttpHandler;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MerlinCLIPlanTests {

    private String resourcesRoot = "src/test/resources";
    private final String test_file_name = "test143.json";
    private final String baseURL = "http://localhost:27183/api/plans";
    private final String activityPath = "activity_instances";
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

    @Test
    public void testReadPlanList() throws IOException, URISyntaxException {
        String body = "[" +
                  "{" +
                    "\"name\": \"Dragon and Bunny\"," +
                    "\"adaptationId\": \"0143-0143-0143-0143\"," +
                    "\"version\": \"143\"," +
                    "\"startTimestamp\": \"2006-356T20:53:00\"" +
                  "}" +
                "]";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "--list-plans" };
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
     * Tests that the CLI issues a GET and writes a file containing
     * the response from the server, when issued -pull
     */
    @Test
    public void testPlanPull() throws IOException, URISyntaxException {

        // Ensure test file doesn't exist before performing the test
        removeFile(test_file_name);
        String planId = "143";
        String body = "{" +
                    "\"status\": \"success\"" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--download-plan", test_file_name};
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, planId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String contents = readFile(test_file_name);
        assertThat(new JSONMatcher(body).matches(contents));

        // Cleanup
        removeFile(test_file_name);
    }

    /**
     * Tests that the CLI issues a POST to the correct endpoint
     * when asked to create a plan.
     *
     * Checks that the body of the POST is the contents of the
     * specified file.
     */
    @Test
    public void testPlanCreation() throws IOException, URISyntaxException {
        String path = String.format("%s/red_apple_plan.json", resourcesRoot);
        String expectedBody = null;
        String location = "143";

        expectedBody = readFile(path);

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_CREATED, "");
        response.setHeader("location", location);

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-P", path };
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

        String requestBody = new String(((HttpPost)request).getEntity().getContent().readAllBytes());
        assertThat(requestBody).isEqualTo(expectedBody);
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update a plan by file
     *
     * Checks the body of the PATCH matches the file supplied to
     * the CLI
     */
    @Test
    public void testPlanFileUpdate() throws IOException, URISyntaxException {
        String planId = "137";
        String path = String.format("%s/green_apple_plan.json", resourcesRoot);
        String expectedBody = readFile(path);

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--update-plan-from-file", path };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, planId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpPatch.METHOD_NAME);

        String requestBody = new String(((HttpPatch)request).getEntity().getContent().readAllBytes());
        assertThat(requestBody).isEqualTo(expectedBody);
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update a plan.
     *
     * Checks that the body of the PATCH contains the expected
     * JSON based on the command issued.
     */
    @Test
    public void testPlanUpdate() throws URISyntaxException, IOException {
        String planId = "23598";
        String expectedBody = "{" +
                    "adaptationId: \"2\"," +
                    "startTimestamp: \"2018-331T12:00:00\"" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--update-plan", "adaptationId=2", "startTimestamp=2018-331T12:00:00" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, planId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpPatch.METHOD_NAME);

        String requestBody = new String(((HttpPatch)request).getEntity().getContent().readAllBytes());
        assertThat(new JSONMatcher(expectedBody).matches(requestBody)).isTrue();
    }

    /**
     * Tests that the CLI issues a DELETE to the correct endpoint
     * when asked to delete a plan.
     */
    @Test
    public void testPlanDelete() throws URISyntaxException {
        String planId = "485";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--delete-plan" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s", baseURL, planId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpDelete.METHOD_NAME);
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked to get an activity instance
     *
     * Checks that the output of the CLI contains the expected
     * JSON (Assumes first '{' is start of JSON)
     */
    @Test
    public void testActivityRead() throws URISyntaxException, IOException {
        String planId = "242f1a";
        String activityId = "38fshg7g5";
        String body = "{" +
                    "\"status\": \"success\"" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");
        response.setEntity(new StringEntity(body));

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--display-activity", activityId };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpGet.METHOD_NAME);

        String output = outContent.toString();
        assertThat(output.indexOf('{')).isGreaterThanOrEqualTo(0);

        String result = output.substring(output.indexOf('{'));
        assertThat(new JSONMatcher(body).matches(result)).isTrue();
    }

    /**
     * Tests that the CLI issues a POST to the correct endpoint
     * when asked to create an activity instance
     *
     * Checks that the body of the POST is the contents of the
     * specified file.
     */
    @Test
    public void testAppendActivities() throws URISyntaxException, IOException {
        String planId = "s38j2";
        String path = String.format("%s/rotten_apple_activity.json", resourcesRoot);
        String expectedBody = null;
        URI location = new URI("2xcv0x");
        expectedBody = readFile(path);

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_CREATED, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--append-activities", path };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s", baseURL, planId, activityPath));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpPost.METHOD_NAME);

        String requestBody = new String(((HttpPost)request).getEntity().getContent().readAllBytes());
        assertThat(new JSONMatcher(expectedBody).matches(requestBody)).isTrue();
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update an activity instance
     *
     * Checks that the body of the PATCH contains the expected
     * JSON based on the command issued
     */
    @Test
    public void testActivityUpdate() throws URISyntaxException, IOException {
        String planId = "2j24j";
        String activityId = "65erg";

        String expectedBody = "{" +
                    "\"startTimestamp\": \"2018-331T04:00:00\"," +
                    "\"parameters\" : [" +
                        "{ \"name\": \"color\", \"value\": \"purple\" }," +
                        "{ \"name\": \"age\"  , \"value\": \"7\"      }"  +
                    "]" +
                "}";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--update-activity", activityId, "param:color=purple", "startTimestamp=2018-331T04:00:00", "param:age=7" };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpPatch.METHOD_NAME);

        String requestBody = new String(((HttpPatch)request).getEntity().getContent().readAllBytes());
        assertThat(new JSONMatcher(expectedBody).matches(requestBody)).isTrue();
    }

    @Test
    public void testActivityDelete() throws URISyntaxException {
        String planId = "4ob3w";
        String activityId = "b4e89";

        // 1. Create a new response
        BasicHttpResponse response = new BasicHttpResponse(null, HttpStatus.SC_OK, "");

        // 2. Register the response with the Mock Client
        this.mockHttpHandler.setNextResponse(response);

        // 3. Make the request
        String[] args = { "-p", planId, "--delete-activity", activityId };
        CommandOptions commandOptions = new CommandOptions(args, mockHttpHandler);
        commandOptions.parse();

        // 4. Get the request
        HttpUriRequest request = this.mockHttpHandler.getLastRequest();

        // 5. Validate the request contained the expected information
        assertThat(commandOptions.lastCommandSuccessful()).isTrue();

        URI uri = new URI(String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId));
        assertThat(request.getURI()).isEqualTo(uri);

        String method = request.getMethod();
        assertThat(method).isEqualTo(HttpDelete.METHOD_NAME);
    }

    /**
     * Remove file if it exists
     *
     * Returns true if the file does not exist or was delted
     * otherwise false
     */
    public boolean removeFile(String path) {
        File file = new File(path);

        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    /**
     * Read contents of a file into a string
     */
    public String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }
}
