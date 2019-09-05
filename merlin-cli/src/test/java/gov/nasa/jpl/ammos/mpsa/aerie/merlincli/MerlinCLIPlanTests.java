package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JSONMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.TestCase.fail;
import static junit.framework.TestCase.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ContextConfiguration(locations = {"classpath:/applicationContext-test.xml"})
public class MerlinCLIPlanTests extends AbstractJUnit4SpringContextTests {

    private String resourcesRoot = "src/test/resources";
    private final String test_file_name = "test38294582.json";
    private final String baseURL = "http://localhost:27183/api/plans";
    private final String activityPath = "activity_instances";
    private MockRestServiceServer mockServer;

    // Used to intercept System.out and System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Autowired
    private CommandOptions commandOptions;

    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testReadPlanList() {
        String body = "[" +
                  "{" +
                    "\"name\": \"testPlan\"," +
                    "\"adaptationId\": \"1234-5678-9abc-def0\"," +
                    "\"version\": \"0.4\"," +
                    "\"startTimestamp\": \"2018-331T11:00:00\"" +
                  "}" +
                "]";

        mockServer.expect(requestTo(baseURL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "--list-plans" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('['));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    /**
     * Tests that the CLI issues a GET and writes a file containing
     * the response from the server, when issued -pull
     */
    @Test
    public void testPlanPull() {

        // Ensure test file doesn't exist before performing the test
        removeFile(test_file_name);
        String planId = "234";
        String body = "{" +
                    "\"status\": \"success\"" +
                "}";

        String url = String.format("%s/%s", baseURL, planId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-p", planId, "--download-plan", test_file_name};
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        try {
            String contents = readFile(test_file_name);

            assertTrue(new JSONMatcher(body).matches(contents));

            // Cleanup
            removeFile(test_file_name);
        } catch (IOException e) {
            fail("Plan pull failed.");
        }
    }

    /**
     * Tests that the CLI issues a POST to the correct endpoint
     * when asked to create a plan.
     *
     * Checks that the body of the POST is the contents of the
     * specified file.
     */
    @Test
    public void testPlanCreation() {
        String path = String.format("%s/red_apple_plan.json", resourcesRoot);
        String expectedBody = null;
        URI location = null;
        try {
            location = new URI("453");
            expectedBody = readFile(path);
        } catch (URISyntaxException | IOException e) {
            fail(e.getMessage());
        }
        mockServer.expect(requestTo(baseURL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(new JSONMatcher(expectedBody)))
                .andRespond(withCreatedEntity(location));

        String[] args = { "-P", path };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update a plan by file
     *
     * Checks the body of the PATCH matches the file supplied to
     * the CLI
     */
    @Test
    public void testPlanFileUpdate() {

        String planId = "137";
        String path = String.format("%s/green_apple_plan.json", resourcesRoot);
        String expectedBody = null;
        try {
            expectedBody = readFile(path);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String url = String.format("%s/%s", baseURL, planId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(new JSONMatcher(expectedBody)))
                .andRespond(withNoContent());

        String[] args = { "-p", planId, "--update-plan-from-file", path };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update a plan.
     *
     * Checks that the body of the PATCH contains the expected
     * JSON based on the command issued.
     */
    @Test
    public void testPlanUpdate() {
        String planId = "23598";
        String expectedBody = "{" +
                    "adaptationId: \"2\"," +
                    "startTimestamp: \"2018-331T12:00:00\"" +
                "}";

        String url = String.format("%s/%s", baseURL, planId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(new JSONMatcher(expectedBody)))
                .andRespond(withNoContent());

        String[] args = { "-p", planId, "--update-plan", "adaptationId=2", "startTimestamp=2018-331T12:00:00" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    /**
     * Tests that the CLI issues a DELETE to the correct endpoint
     * when asked to delete a plan.
     */
    @Test
    public void testPlanDelete() {
        String planId = "485";

        String url = String.format("%s/%s", baseURL, planId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        String[] args = { "-p", planId, "--delete-plan" };
        commandOptions.consumeArgs(args).parse();
        assertTrue(commandOptions.lastCommandSuccessful());

        mockServer.verify();
    }

    /**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked to get an activity instance
     *
     * Checks that the output of the CLI contains the expected
     * JSON (Assumes first '{' is start of JSON)
     */
    @Test
    public void testActivityRead() {
        String planId = "242f1a";
        String activityId = "38fshg7g5";
        String body = "{" +
                    "\"status\": \"success\"" +
                "}";

        String url = String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-p", planId, "--display-activity", activityId };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('{'));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    /**
     * Tests that the CLI issues a POST to the correct endpoint
     * when asked to create an activity instance
     *
     * Checks that the body of the POST is the contents of the
     * specified file.
     */
    @Test
    public void testAppendActivities() {
        String planId = "s38j2";
        String path = String.format("%s/rotten_apple_activity.json", resourcesRoot);
        String expectedBody = null;
        URI location = null;
        try {
            location = new URI("2xcv0x");
            expectedBody = readFile(path);
        } catch (URISyntaxException | IOException e) {
            fail(e.getMessage());
        }

        String url = String.format("%s/%s/%s", baseURL, planId, activityPath);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(new JSONMatcher(expectedBody)))
                .andRespond(withCreatedEntity(location));

        String[] args = { "-p", planId, "--append-activities", path };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    /**
     * Tests that the CLI issues a PATCH to the correct endpoint
     * when asked to update an activity instance
     *
     * Checks that the body of the PATCH contains the expected
     * JSON based on the command issued
     */
    @Test
    public void testActivityUpdate() {
        String planId = "2j24j";
        String activityId = "65erg";

        // TODO: Range should be removed from activity instance parameters (it belongs in types only)
        // TODO: It would be better if we didn't send empty lists for constraints and listeners
        String expectedBody = "{" +
                    "\"start\": 7.23," +
                    "\"constraints\": []," +
                    "\"listeners\"  : []," +
                    "\"parameters\" : [" +
                        "{ \"name\": \"color\", \"value\": \"purple\" }," +
                        "{ \"name\": \"age\"  , \"value\": \"7\"      }"  +
                    "]" +
                "}";

        String url = String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(new JSONMatcher(expectedBody)))
                .andRespond(withNoContent());

        String[] args = { "-p", planId, "--update-activity", activityId, "param:color=purple", "start=7.23", "param:age=7" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    @Test
    public void testActivityDelete() {
        String planId = "4ob3w";
        String activityId = "b4e89";

        String url = String.format("%s/%s/%s/%s", baseURL, planId, activityPath, activityId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        String[] args = { "-p", planId, "--delete-activity", activityId };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
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
