package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Test that the repository and database work together
 */
@ExtendWith(SpringExtension.class)
@DataMongoTest
public class PlansRepositoryIntegrationTest {

    @Autowired
    private PlansRepository plansRepository;

    @Test
    public void testCreatePlan() {
        PlanDetail plan = new PlanDetail("1", "2008-05-11T15:30:00", null, "MyAdaptation", "2007-03-01T13:00:00Z", new ArrayList<>());
        PlanDetail response = plansRepository.save(plan);
        assertThat(response.getName()).isEqualTo(plan.getName());
    }

    @Test
    public void testGetPlanById() {
        PlanDetail plan = new PlanDetail("1", "2008-05-11T15:30:00", null, "MyAdaptation", "2007-03-01T13:00:00Z", new ArrayList<>());

        plansRepository.save(plan);
        assertThat(plan.getId()).isNotNull();

        Optional<PlanDetail> foundPlan = plansRepository.findById(plan.getId());
        assertThat(foundPlan.isPresent()).isTrue();
        assertThat(foundPlan.get().getName()).isEqualTo(plan.getName());
        assertThat(foundPlan.get().getId()).isEqualTo(plan.getId());
    }
}
