package gov.nasa.jpl.aerie.e2e.procedural.scheduling;

import gov.nasa.jpl.aerie.e2e.ExternalDatasetsTest;
import gov.nasa.jpl.aerie.e2e.types.GoalInvocationId;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalEventsTests extends ProceduralSchedulingSetup {
  private GoalInvocationId procedureId;
  private int datasetId;

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

    // TODO pranav add a method to the HasuraRequests class for uploading events
    // TODO pranav upload some events
  }

  @AfterEach
  void localAfterEach() throws IOException {
    hasura.deleteSchedulingGoal(procedureId.goalId());
    hasura.deleteExternalDataset(planId, datasetId);

    // TODO pranav add a method to HasuraRequests for deleting events
    // TODO pranav delete the events added in localBeforeEach
  }

  @Test
  void testExternalEventQuery() throws IOException {
    hasura.awaitScheduling(specId);

    final var plan = hasura.getPlan(planId);
    final var activities = plan.activityDirectives();

    // TODO pranav make sure the ExternalEventsGoal successfully made Bite Banana activities
    // one-to-one with the uploaded events.
  }
}
