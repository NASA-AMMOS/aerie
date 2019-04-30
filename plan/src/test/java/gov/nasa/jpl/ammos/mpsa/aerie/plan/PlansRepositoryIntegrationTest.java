package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

/**
 * Test that the repository and database work together
 */
@RunWith(SpringRunner.class)
@DataMongoTest
public class PlansRepositoryIntegrationTest {

    @Autowired
    private PlansRepository plansRepository;

    @Test
    public void testCreatePlan() {
        Plan plan = new Plan("1", "2008-05-11T15:30:00", null, "MyAdaptation", "2007-03-01T13:00:00Z");
        Plan response = plansRepository.save(plan);
        Assert.assertEquals(response.getName(), plan.getName());
    }

    @Test
    public void testGetPlanById() {
        Plan plan = new Plan("1", "2008-05-11T15:30:00", null, "MyAdaptation", "2007-03-01T13:00:00Z");

        plansRepository.save(plan);
        Assert.assertNotNull(plan.getId());

        Optional<Plan> foundPlan = plansRepository.findById(plan.getId());
        Assert.assertTrue(foundPlan.isPresent());
        Assert.assertEquals(foundPlan.get().getName(), plan.getName());
        Assert.assertEquals(foundPlan.get().getId(), plan.getId());
    }
}
