package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JSONMatcher;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;


public class MerlinCLIAdaptationTests {
    private String resourcesRoot = "src/test/resources";
        private final String baseURL = "http://localhost:27182/api/adaptations";

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
    /*@Test
    public void testAdaptationCreation() {
        String path = String.format("%s/bananatation.jar", resourcesRoot);
        String name = "testName";
        String version = "1.0a.3";
        URI location = null;
        CreateAdaptationRequestMatcher matcher = new CreateAdaptationRequestMatcher(path, name, version, null, null);

        try {
            location = new URI("427");
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }

        // TODO: We should increase the rigor of this test, but it's not clear how to do that. Leaving it as a
        // TODO, since we are probably going to pull out the use of the Spring framework anyway
        mockServer.expect(requestTo(baseURL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(StringContains.containsString("filename=\"bananatation.jar\"")))
                .andRespond(withCreatedEntity(location));

        String[] args = { "--create-adaptation", path, "name="+name, "version="+version };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());
    }

    *//**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for a list of adaptations.
     *
     * Checks that the response is printed to standard out
     *//*
    @Test
    public void testReadAdaptationList() {

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

        mockServer.expect(requestTo(baseURL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "--list-adaptations" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('['));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    *//**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an adaptation by ID.
     *
     * Checks that the response is printed to standard out
     *//*
    @Test
    public void testReadAdaptation() {
        String adaptationId = "0123-4567-89ab-cdef";
        String body = "{" +
                  "\"name\": \"Test\"," +
                  "\"version\": \"0.a\"," +
                  "\"mission\": \"Impossible\"," +
                  "\"owner\": \"Tom Cruise\"" +
                "}";

        String url = String.format("%s/%s", baseURL, adaptationId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-a", adaptationId, "--view-adaptation" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('{'));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    *//**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an adaptation's activity types
     *
     * Checks that the response is printed to standard out
     *//*
    @Test
    public void testReadActivityTypes() {
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

        String url = String.format("%s/%s/activities", baseURL, adaptationId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-a", adaptationId, "--activity-types" };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('['));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    *//**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an activity type from an adaptation
     *
     * Checks that the response is printed to standard out
     *//*
    @Test
    public void testReadActivityType() {
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

        String url = String.format("%s/%s/activities/%s", baseURL, adaptationId, activityId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-a", adaptationId, "--activity-type", activityId };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('{'));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    *//**
     * Tests that the CLI issues a GET to the correct endpoint
     * when asked for an activity type's parameters
     *
     * Checks that the response is printed to standard out
     *//*
    @Test
    public void testReadActivityTypeParameters() {
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

        String url = String.format("%s/%s/activities/%s/parameters", baseURL, adaptationId, activityId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String[] args = { "-a", adaptationId, "--activity-type-parameters", activityId };
        commandOptions.consumeArgs(args).parse();

        mockServer.verify();
        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        String result = output.substring(output.indexOf('['));
        assertTrue(new JSONMatcher(body).matches(result));
    }

    *//**
     * Tests that the CLI issues a DELETE to the correct endpoint
     * when asked to delete an adaptation
     *//*
    @Test
    public void testDeleteAdaptation() {
        String adaptationId = "485a-36a9=bc7e-7fec";

        String url = String.format("%s/%s", baseURL, adaptationId);
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        String[] args = { "-a", adaptationId, "--delete-adaptation" };
        commandOptions.consumeArgs(args).parse();
        assertTrue(commandOptions.lastCommandSuccessful());

        mockServer.verify();
    }*/
}