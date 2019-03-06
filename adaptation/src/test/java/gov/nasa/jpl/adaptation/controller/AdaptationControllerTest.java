package gov.nasa.jpl.adaptation.controller;

import gov.nasa.jpl.adaptation.Adaptation;
import gov.nasa.jpl.adaptation.AdaptationRepository;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@WebMvcTest(value = AdaptationController.class, secure = false)
public class AdaptationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdaptationRepository repository;


    private static Adaptation mockAdaptationId = null;
    private static List<Adaptation> mockAdaptations = new ArrayList<Adaptation>();

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

}
