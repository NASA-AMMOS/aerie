package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidEntityException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.PlanDeserializer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonValue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PlanDeserializerTests {

    @Test
    public void testDeserializeActivityParameter() throws InvalidEntityException {
        JsonValue parameterJson = Json.createValue(9.3);

        SerializedParameter parameter = PlanDeserializer.deserializeActivityParameter(parameterJson);

        assertThat(parameter).isEqualTo(SerializedParameter.of(9.3));
    }

    @Test
    public void testDeserializeActivity() throws InvalidEntityException {
        JsonValue activityJson = Json.createObjectBuilder()
                .add("activityType", "PeelBanana")
                .add("parameters", Json.createObjectBuilder()
                        .add("peelDirection", "fromStem")
                )
                .add("startTimestamp", "2018-331T04:00:00")
                .build();

        ActivityInstance activity = PlanDeserializer.deserializeActivityInstance(activityJson);

        assertThat(activity.getActivityType()).isEqualTo("PeelBanana");
        assertThat(activity.getParameters().size()).isEqualTo(1);
        assertThat(activity.getParameters().get("peelDirection")).isNotNull();
        assertThat(activity.getParameters().get("peelDirection")).isEqualTo(SerializedParameter.of("fromStem"));
        assertThat(activity.getStartTimestamp()).isEqualTo("2018-331T04:00:00");
    }

    @Test
    public void testDeserializePlan() throws InvalidEntityException {
        JsonValue planJson = Json.createObjectBuilder()
                .add("adaptationId", "testAdaptationId")
                .add("name", "testPlan")
                .add("startTimestamp", "2018-331T00:00:00")
                .add("endTimestamp", "2018-332T00:00:00")
                .add("activityInstances", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("activityType", "PeelBanana")
                                .add("parameters", Json.createObjectBuilder()
                                                .add("peelDirection", "fromStem")
                                )
                                .add("startTimestamp", "2018-331T04:00:00")
                                .build()
                        )
                        .add(Json.createObjectBuilder()
                                .add("activityType", "BiteBanana")
                                .add("parameters", Json.createObjectBuilder()
                                        .add("biteSize", 7)
                                )
                                .add("startTimestamp", "2018-331T04:30:00")
                                .build()
                        )
                        .build()
                )
                .build();

        PlanDetail plan = PlanDeserializer.deserializePlan(planJson);

        assertThat(plan.getAdaptationId()).isEqualTo("testAdaptationId");
        assertThat(plan.getName()).isEqualTo("testPlan");
        assertThat(plan.getStartTimestamp()).isEqualTo("2018-331T00:00:00");
        assertThat(plan.getEndTimestamp()).isEqualTo("2018-332T00:00:00");
        assertThat(plan.getActivityInstances().size()).isEqualTo(2);

        ActivityInstance activity1 = plan.getActivityInstances().get(0);
        ActivityInstance activity2 = plan.getActivityInstances().get(1);

        assertThat(activity1).isNotNull();
        assertThat(activity1.getActivityType()).isEqualTo("PeelBanana");
        assertThat(activity1.getParameters().size()).isEqualTo(1);
        assertThat(activity1.getParameters().get("peelDirection")).isNotNull();
        assertThat(activity1.getParameters().get("peelDirection")).isEqualTo(SerializedParameter.of("fromStem"));
        assertThat(activity1.getStartTimestamp()).isEqualTo("2018-331T04:00:00");

        assertThat(activity2).isNotNull();
        assertThat(activity2.getActivityType()).isEqualTo("BiteBanana");
        assertThat(activity2.getParameters().size()).isEqualTo(1);
        assertThat(activity2.getParameters().get("biteSize")).isNotNull();
        assertThat(activity2.getParameters().get("biteSize")).isEqualTo(SerializedParameter.of(7));
        assertThat(activity2.getStartTimestamp()).isEqualTo("2018-331T04:30:00");
    }
}
