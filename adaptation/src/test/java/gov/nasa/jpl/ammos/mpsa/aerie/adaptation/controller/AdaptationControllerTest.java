package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.AdaptationRepository;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    private static int delete_me_id = 484;
    private static String delete_me_path = "delete_me.txt";

    @BeforeClass
    public static void setupMocks() {
        /*
        Doing it this way because there is no constructor to add an ID... Reason being, we are letting the database to
        generate an identifier.
         */

        Adaptation a1 = new Adaptation("MockAdaptation", "1.0.0", "UnitTester", "TestingMission", "somewhere/local");
        a1.setId(1);

        mockAdaptationId = a1;

        Adaptation a2 = new Adaptation("MockAdaptation2", "1.0.0", "UnitTester", "TestingMission", "somewhere/local/two");
        a1.setId(2);
        Adaptation a3 = new Adaptation("MockAdaptation3", "2.0.1", "UnitTester", "TestingMission", "somewhere/local/three");
        a1.setId(3);

        mockAdaptations.add(a1);
        mockAdaptations.add(a2);
        mockAdaptations.add(a3);

        // Create adaptation for delete test
        delete_me_adaptation = new Adaptation("DeleteMe", "1.0.0", "UnitTester", "TestingMission", delete_me_path);
        delete_me_adaptation.setId(delete_me_id);
        try {
            File delete_file = new File(delete_me_path);
            delete_file.createNewFile();
        } catch (IOException e) {
            System.err.println("Unable to create delete_me.txt. Delete test will fail.");
        }
    }

    @AfterClass
    public static void teardownSetup() {
        File file = new File(delete_me_path);
        if (file.exists()) {
            file.delete();
        }
    }

    @Ignore
    @Test
    public void retrieveAdaptations() throws Exception {

        Mockito.when(repository.findAll()).thenReturn(mockAdaptations);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/api/adaptation");

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        String expected = "[{\"id\":3,\"name\":\"MockAdaptation\",\"location\":\"somewhere/local\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"1.0.0\"},{\"id\":null,\"name\":\"MockAdaptation2\",\"location\":\"somewhere/local/two\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"1.0.0\"},{\"id\":null,\"name\":\"MockAdaptation3\",\"location\":\"somewhere/local/three\",\"owner\":\"UnitTester\",\"mission\":\"TestingMission\",\"version\":\"2.0.1\"}]";

        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test

    public void deleteAdaptation() throws Exception {
        Optional<Adaptation> optAdapt = Optional.of(delete_me_adaptation);
        Mockito.when(repository.findById(delete_me_id)).thenReturn(Optional.of(delete_me_adaptation));

        File file = new File(delete_me_path);
        Assert.assertEquals(file.exists(), true);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.delete("/adaptations/" + delete_me_id);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertEquals(result.getResponse().getStatus(), 200);
        Assert.assertEquals(file.exists(), false);

        requestBuilder = MockMvcRequestBuilders.delete("/adaptations/" + (delete_me_id + 1));
        result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertEquals(result.getResponse().getStatus(), 404);
    }

}
