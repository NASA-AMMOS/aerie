package gov.nasa.jpl.aerie.e2e.procedural.scheduling;

import gov.nasa.jpl.aerie.e2e.ExternalDatasetsTest;
import gov.nasa.jpl.aerie.e2e.types.GoalInvocationId;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import gov.nasa.jpl.aerie.e2e.utils.HasuraRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.JsonArray;
import java.io.IOException;
import java.util.List;

public class ExternalEventsTests extends ProceduralSchedulingSetup {
  private GoalInvocationId procedureId;
  private int datasetId;

  private final static String SOURCE_TYPE = "TestType";
  private final static String EVENT_TYPE = "TestType";
  private final static String DERIVATION_GROUP = "TestGroup";

  private final HasuraRequests.ExternalSource externalSource = new HasuraRequests.ExternalSource(
      "Test.json",
      SOURCE_TYPE,
      DERIVATION_GROUP,
      "2024-01-01T00:00:00Z",
      "2024-01-01T00:00:00Z",
      "2024-01-08T00:00:00Z",
      "2024-10-01T00:00:00Z"
  );
  private final List<HasuraRequests.ExternalEvent> externalEvents = List.of(
      new HasuraRequests.ExternalEvent(
          "Event_01",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2024-01-01T01:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_02",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2024-01-01T03:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_03",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2024-01-01T05:00:00Z",
          "00:10:00"
      )
  );

  @BeforeEach
  void localBeforeEach() throws IOException {
    try (final var gateway = new GatewayRequests(playwright)) {
      int procedureJarId = gateway.uploadJarFile("build/libs/ExternalEventsGoal.jar");
      // Add Scheduling Procedure
      procedureId = hasura.createSchedulingSpecProcedure(
          "Test Scheduling Procedure",
          procedureJarId,
          specId,
          0
      );

      datasetId = hasura.insertExternalDataset(
          planId,
          "2023-001T01:00:00.000",
          List.of(ExternalDatasetsTest.myBooleanProfile)
      );
    }

    // Upload some External Events (and associated infrastructure)
    hasura.insertExternalSourceType(SOURCE_TYPE);
    hasura.insertExternalEventType(EVENT_TYPE);
    hasura.insertDerivationGroup(DERIVATION_GROUP, SOURCE_TYPE);
    hasura.insertExternalSource(externalSource);
    hasura.insertExternalEvents(externalEvents);

    // Note: we do not need to manually delete this. The plan is deleted first in ProceduralSchedulingSetup.java's
    //    @AfterEach method, which cascades and deletes this. Attempting deletion will cause error.
    hasura.insertPlanDerivationGroupAssociation(planId, DERIVATION_GROUP);
  }

  @AfterEach
  void localAfterEach() throws IOException {
    hasura.deleteSchedulingGoal(procedureId.goalId());
    hasura.deleteExternalDataset(planId, datasetId);

    // External Event Related
    hasura.deletePlanDerivationGroup(planId, DERIVATION_GROUP);
    hasura.deleteExternalSource(externalSource);
    hasura.deleteDerivationGroup(DERIVATION_GROUP);
    hasura.deleteExternalSourceType(SOURCE_TYPE);
    hasura.deleteExternalEventType(EVENT_TYPE);
  }

  @Test
  void testExternalEventQuery() throws IOException {
    hasura.awaitScheduling(specId);

    final var plan = hasura.getPlan(planId);
    final var activities = plan.activityDirectives();

    // TODO pranav make sure the ExternalEventsGoal successfully made Bite Banana activities one-to-one with the uploaded events.
  }

  // TODO pranav add another goal that filters by type and group, test it here
}
