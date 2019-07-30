package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.Repositories.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest(value = AdaptationController.class, secure = false)
public class AdaptationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdaptationRepository repository;


    private static Adaptation mockAdaptationId = null;
    private static List<Adaptation> mockAdaptations = new ArrayList<Adaptation>();
    private static Adaptation delete_me_adaptation = null;
    private static String delete_me_id = "06045376-b2f7-11e9-a2a3-2a2ae2dbcce4";
    private static String delete_me_path = "delete_me.txt";
    private static Adaptation fullAdaptation = null;

    @BeforeClass
    public static void setupMocks() {
        /*
        Doing it this way because there is no constructor to add an ID... Reason being, we are letting the database to
        generate an identifier.
         */

        Adaptation a1 = new Adaptation("e3bd5a42-b2f6-11e9-a2a3-2a2ae2dbcce4", "MockAdaptation1", "1.0.0", "UnitTester", "TestingMission", "somewhere/local");

        mockAdaptationId = a1;

        Adaptation a2 = new Adaptation("e3bd5a42-b2f6-11e9-a2a3-2a2ae2dbcce4", "MockAdaptation2", "1.0.0", "UnitTester", "TestingMission", "somewhere/local/two");
        Adaptation a3 = new Adaptation("e3bd5a42-b2f6-11e9-a2a3-2a2ae2dbcce4", "MockAdaptation3", "2.0.1", "UnitTester", "TestingMission", "somewhere/local/three");

        mockAdaptations.add(a1);
        mockAdaptations.add(a2);
        mockAdaptations.add(a3);

        // Create adaptation for delete test
        delete_me_adaptation = new Adaptation(delete_me_id, "DeleteMe", "1.0.0", "UnitTester", "TestingMission", delete_me_path);

        try {
            File delete_file = new File(delete_me_path);
            delete_file.createNewFile();
        } catch (IOException e) {
            System.err.println("Unable to create delete_me.txt. Delete test will fail.");
        }

        List<ActivityType> activityTypes = new ArrayList<>();

        List<ActivityTypeParameter> act1Parameters = new ArrayList<>();
        act1Parameters.add(new ActivityTypeParameter("param1", "type1"));
        act1Parameters.add(new ActivityTypeParameter("param2", "type2"));
        activityTypes.add(new ActivityType("13d77a34-b2ff-11e9-a2a3-2a2ae2dbcce4", "activity1", act1Parameters));

        List<ActivityTypeParameter> act2Parameters = new ArrayList<>();
        act2Parameters.add(new ActivityTypeParameter("param3", "type3"));
        act2Parameters.add(new ActivityTypeParameter("param4", "type4"));
        activityTypes.add(new ActivityType("13d777c8-b2ff-11e9-a2a3-2a2ae2dbcce4", "activity2", act2Parameters));

        fullAdaptation = new Adaptation("0a074a3e-b2ff-11e9-a2a3-2a2ae2dbcce4", "name", "version", "owner", "mission", "location", activityTypes);

    }

    @AfterClass
    public static void teardownSetup() {
        File file = new File(delete_me_path);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void getAdaptation() throws Exception {
        Adaptation adaptation = mockAdaptations.get(0);
        Mockito.when(repository.findById(adaptation.getId())).thenReturn(Optional.of(adaptation));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/adaptations/" + adaptation.getId());

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = String.format("{\"name\":\"MockAdaptation1\",\"id\":\"%s\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"1.0.0\"},", mockAdaptations.get(0).getId());

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void getAllAdaptations() throws Exception {

        Mockito.when(repository.findAllProjectedBy()).thenReturn(MockAdaptationProjection.convertAdaptationList(mockAdaptations));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/adaptations");

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = String.format("[" +
                          "{\"name\":\"MockAdaptation1\",\"id\":\"%s\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"1.0.0\"}," +
                          "{\"name\":\"MockAdaptation2\",\"id\":\"%s\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"1.0.0\"}," +
                          "{\"name\":\"MockAdaptation3\",\"id\":\"%s\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"2.0.1\"}" +
                        "]",
                mockAdaptations.get(0).getId(),
                mockAdaptations.get(1).getId(),
                mockAdaptations.get(2).getId()
        );

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Ignore /* TODO: Need to figure out how to put together a multipart request for the post */
    @Test
    public void postAdaptation() throws Exception {
        Mockito.when(repository.findAll()).thenReturn(mockAdaptations);

        MockMultipartFile file = new MockMultipartFile("jar", "adaptation.jar", "application/java-archive", "".getBytes());
        RequestBuilder requestBuilder = MockMvcRequestBuilders.multipart("/adaptations")
                .file(file);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertEquals(201, result.getResponse().getStatus());
    }

    @Test
    public void deleteAdaptation() throws Exception {
        Optional<Adaptation> optAdapt = Optional.of(delete_me_adaptation);
        Mockito.when(repository.findById(delete_me_id)).thenReturn(Optional.of(delete_me_adaptation));

        File file = new File(delete_me_path);
        Assert.assertEquals(file.exists(), true);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/adaptations/" + delete_me_id);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertEquals(200, result.getResponse().getStatus());
        Assert.assertEquals(false, file.exists());

        requestBuilder = MockMvcRequestBuilders.delete("/adaptations/07045326-b2f7-11e9-a2a3-2a2ae2dbcde5");
        result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertEquals(404, result.getResponse().getStatus());
    }

    @Test
    public void getActivityTypes() throws Exception {
        ActivityType activity1 = fullAdaptation.getActivityTypes().get(0);
        ActivityType activity2 = fullAdaptation.getActivityTypes().get(1);
        Mockito.when(repository.findById(fullAdaptation.getId())).thenReturn(Optional.of(fullAdaptation));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(String.format("/adaptations/%s/activities", fullAdaptation.getId()));

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = String.format("[{\"id\":\"%s\",\"name\":\"activity1\",\"parameters\":[{\"name\":\"param1\",\"type\":\"type1\"},{\"name\":\"param2\",\"type\":\"type2\"}]}," +
                "{\"id\":\"%s\",\"name\":\"activity2\",\"parameters\":[{\"name\":\"param3\",\"type\":\"type3\"},{\"name\":\"param4\",\"type\":\"type4\"}]}]", activity1.getId(), activity2.getId());

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void getActivityTypeById() throws Exception {
        ActivityType activity = fullAdaptation.getActivityTypes().get(0);
        Mockito.when(repository.findById(fullAdaptation.getId())).thenReturn(Optional.of(fullAdaptation));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(String.format("/adaptations/%s/activities/%s", fullAdaptation.getId(), activity.getId()));

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = String.format("{\"id\":\"%s\",\"name\":\"activity1\",\"parameters\":[{\"name\":\"param1\",\"type\":\"type1\"},{\"name\":\"param2\",\"type\":\"type2\"}]}", activity.getId());

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void getActivityTypeParameters() throws Exception {
        ActivityType activity = fullAdaptation.getActivityTypes().get(1);
        Mockito.when(repository.findById(fullAdaptation.getId())).thenReturn(Optional.of(fullAdaptation));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(String.format("/adaptations/%s/activities/%s/parameters", fullAdaptation.getId(), activity.getId()));

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = String.format("[{\"name\":\"param3\",\"type\":\"type3\"},{\"name\":\"param4\",\"type\":\"type4\"}]");

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

}

class MockAdaptationProjection implements AdaptationProjection {

    private Adaptation adaptation;

    public MockAdaptationProjection(Adaptation adaptation) {
        this.adaptation = adaptation;
    }

    @Override
    public String getId() {
        return adaptation.getId();
    }

    @Override
    public String getName() {
        return adaptation.getName();
    }

    @Override
    public String getVersion() {
        return adaptation.getVersion();
    }

    @Override
    public String getOwner() {
        return adaptation.getOwner();
    }

    @Override
    public String getMission() {
        return adaptation.getMission();
    }

    public static List<AdaptationProjection> convertAdaptationList(List<Adaptation> adaptations) {
        List<AdaptationProjection> projections = new ArrayList<>();

        for (var adaptation : adaptations) {
            projections.add(new MockAdaptationProjection(adaptation));
        }

        return projections;
    }
}
