package gov.nasa.jpl.aerie.e2e.procedural.scheduling;

import gov.nasa.jpl.aerie.e2e.types.GoalInvocationId;
import gov.nasa.jpl.aerie.e2e.types.Plan;
import gov.nasa.jpl.aerie.e2e.utils.GatewayRequests;
import gov.nasa.jpl.aerie.e2e.utils.HasuraRequests;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalEventsTests extends ProceduralSchedulingSetup {
  private GoalInvocationId procedureId;
  private final static String SOURCE_TYPE = "TestType";
  private final static String EVENT_TYPE = "TestType";
  private final static String ADDITIONAL_EVENT_TYPE = EVENT_TYPE + "_2";
  private final static String DERIVATION_GROUP = "TestGroup";
  private final static String ADDITIONAL_DERIVATION_GROUP = DERIVATION_GROUP + "_2";

  private final HasuraRequests.ExternalSource externalSource = new HasuraRequests.ExternalSource(
      "Test.json",
      SOURCE_TYPE,
      DERIVATION_GROUP,
      "2024-01-01T00:00:00Z",
      "2023-01-01T00:00:00Z",
      "2023-01-08T00:00:00Z",
      "2024-10-01T00:00:00Z"
  );
  private final List<HasuraRequests.ExternalEvent> externalEvents = List.of(
      new HasuraRequests.ExternalEvent(
          "Event_01",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2023-01-01T01:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_02",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2023-01-01T03:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_03",
          EVENT_TYPE,
          externalSource.key(),
          externalSource.derivation_group_name(),
          "2023-01-01T05:00:00Z",
          "00:10:00"
      )
  );

  private final HasuraRequests.ExternalSource additionalExternalSource = new HasuraRequests.ExternalSource(
      "NewTest.json",
      SOURCE_TYPE,
      ADDITIONAL_DERIVATION_GROUP,
      "2024-01-02T00:00:00Z",
      "2023-01-01T00:00:00Z",
      "2023-01-08T00:00:00Z",
      "2024-10-01T00:00:00Z"
  );

  private final List<HasuraRequests.ExternalEvent> additionalExternalEvents = List.of(
      new HasuraRequests.ExternalEvent(
          "Event_01",
          EVENT_TYPE,
          additionalExternalSource.key(),
          additionalExternalSource.derivation_group_name(),
          "2023-01-02T01:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_02",
          ADDITIONAL_EVENT_TYPE,
          additionalExternalSource.key(),
          additionalExternalSource.derivation_group_name(),
          "2023-01-02T03:00:00Z",
          "00:10:00"
      ),
      new HasuraRequests.ExternalEvent(
          "Event_03",
          ADDITIONAL_EVENT_TYPE,
          additionalExternalSource.key(),
          additionalExternalSource.derivation_group_name(),
          "2023-01-02T05:00:00Z",
          "00:10:00"
      )
  );

  @BeforeEach
  void localBeforeEach() throws IOException {
    // Upload some External Events (and associated infrastructure)
    hasura.insertExternalSourceType(SOURCE_TYPE);
    hasura.insertExternalEventType(EVENT_TYPE);
    hasura.insertDerivationGroup(DERIVATION_GROUP, SOURCE_TYPE);
    hasura.insertExternalSource(externalSource);
    hasura.insertExternalEvents(externalEvents);
    hasura.insertPlanDerivationGroupAssociation(planId, DERIVATION_GROUP);

    // Upload additional External Events in a different derivation group and of a different type
    hasura.insertExternalEventType(ADDITIONAL_EVENT_TYPE);
    hasura.insertDerivationGroup(ADDITIONAL_DERIVATION_GROUP, SOURCE_TYPE);
    hasura.insertExternalSource(additionalExternalSource);
    hasura.insertExternalEvents(additionalExternalEvents);
    hasura.insertPlanDerivationGroupAssociation(planId, ADDITIONAL_DERIVATION_GROUP);
  }

  @AfterEach
  void localAfterEach() throws IOException {
    hasura.deleteSchedulingGoal(procedureId.goalId());

    // External Event Related
    hasura.deletePlanDerivationGroupAssociation(planId, DERIVATION_GROUP);
    hasura.deletePlanDerivationGroupAssociation(planId, ADDITIONAL_DERIVATION_GROUP);
    hasura.deleteExternalSource(externalSource);
    hasura.deleteExternalSource(additionalExternalSource);
    hasura.deleteDerivationGroup(DERIVATION_GROUP);
    hasura.deleteDerivationGroup(ADDITIONAL_DERIVATION_GROUP);
    hasura.deleteExternalSourceType(SOURCE_TYPE);
    hasura.deleteExternalEventType(EVENT_TYPE);
    hasura.deleteExternalEventType(ADDITIONAL_EVENT_TYPE);
  }

  @Test
  void testExternalEventSimple() throws IOException {
    // first, run the goal
    try (final var gateway = new GatewayRequests(playwright)) {
      int procedureJarId = gateway.uploadJarFile("build/libs/ExternalEventsSimpleGoal.jar");
      // Add Scheduling Procedure
      procedureId = hasura.createSchedulingSpecProcedure(
          "Test Scheduling Procedure",
          procedureJarId,
          specId,
          0
      );
    }
    hasura.awaitScheduling(specId);
    final var plan = hasura.getPlan(planId);
    final var activities = plan.activityDirectives();

    // ensure the order lines up with the events'
    activities.sort(Comparator.comparing(Plan.ActivityDirective::startOffset));

    // compare arrays
    assertEquals(externalEvents.size(), activities.size());
    for (int i = 0; i < activities.size(); i++) {
      Instant activityStartTime = Duration.addToInstant(
          Instant.parse(planStartTimestamp),
          Duration.fromString(activities.get(i).startOffset())
      );
      assertEquals(externalEvents.get(i).start_time(), activityStartTime.toString());
    }
  }

  @Test
  void testExternalEventQuery() throws IOException {
    // first, run the goal
    try (final var gateway = new GatewayRequests(playwright)) {
      int procedureJarId = gateway.uploadJarFile("build/libs/ExternalEventsQueryGoal.jar");
      // Add Scheduling Procedure
      procedureId = hasura.createSchedulingSpecProcedure(
          "Test Scheduling Procedure",
          procedureJarId,
          specId,
          0
      );
    }
    hasura.awaitScheduling(specId);
    final var plan = hasura.getPlan(planId);
    final var activities = plan.activityDirectives();

    // ensure the orderings line up
    activities.sort(Comparator.comparing(Plan.ActivityDirective::startOffset));

    // get the set of events we expect (anything in TestGroup or TestGroup_2, and of type TestType)
    List<HasuraRequests.ExternalEvent> expected = new ArrayList<>();
    expected.addAll(externalEvents);
    expected.addAll(
        additionalExternalEvents.stream()
                                .filter(e -> e.event_type_name().equals(EVENT_TYPE))
                                .toList()
    );

    // explicitly ensure the orderings line up
    expected.sort(Comparator.comparing(HasuraRequests.ExternalEvent::start_time));

    // compare arrays
    assertEquals(expected.size(), activities.size());
    for (int i = 0; i < activities.size(); i++) {
      Instant activityStartTime = Duration.addToInstant(
          Instant.parse(planStartTimestamp),
          Duration.fromString(activities.get(i).startOffset())
      );
      assertEquals(activityStartTime.toString(), expected.get(i).start_time());
    }
  }
}
