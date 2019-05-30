package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class PlanDetailTest {
    @Test
    @DisplayName("Validating a valid plan should succeed")
    public void testValidateValidPlan() throws IOException {
        final PlanDetail plan = new PlanDetail("adaptation-1", "2008-05-11T15:30:00", "id-1", "plan-1", "2007-03-01T13:00:00Z", new ArrayList<>());

        assertThat(Validator.validate(plan)).isTrue();
    }

    @Test
    @DisplayName("Validating a plan with no id should fail")
    public void testValidatePlanWithNoId() throws IOException {
        final PlanDetail plan = new PlanDetail("adaptation-1", "2008-05-11T15:30:00", null, "plan-1", "2007-03-01T13:00:00Z", new ArrayList<>());

        assertThat(Validator.validate(plan)).isFalse();
    }
}
