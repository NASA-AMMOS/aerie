package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.MerlinParsers.activityInstanceIdP;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;

public final class PostgresPlanRepository implements PlanRepository {
  private final DataSource dataSource;

  public PostgresPlanRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Map<PlanId, Plan> getAllPlans() {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getAllPlansAction = new GetAllPlansAction(connection)) {
        final var planRecords = getAllPlansAction.get();
        final var plans = new HashMap<PlanId, Plan>(planRecords.size());

        for (final var record : planRecords) {
          try {
            final var planId = new PlanId(record.id());
            final var activities = getPlanActivities(connection, planId);
            final var arguments = getPlanArguments(connection, planId);

            plans.put(planId, new Plan(
                record.name(),
                Long.toString(record.missionModelId()),
                record.startTime(),
                record.endTime(),
                activities,
                arguments
            ));
          } catch (final NoSuchPlanException ex) {
            // If a plan was removed between getting its record and getting its activities, then the plan
            // no longer exists, so it's okay to swallow the exception and continue
            System.err.println("Plan was removed while retrieving all plans. Continuing without removed plan.");
          }
        }

        return plans;
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get all plans", ex);
    }
  }

  @Override
  public Plan getPlan(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
        final var planRecord = getPlanRecord(connection, planId);
        final var activities = getPlanActivities(connection, planId);
        final var arguments = getPlanArguments(connection, planId);

        return new Plan(
            planRecord.name(),
            Long.toString(planRecord.missionModelId()),
            planRecord.startTime(),
            planRecord.endTime(),
            activities,
            arguments
        );
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan", ex);
    }
  }

  private Map<String, SerializedValue> getPlanArguments(
      final Connection connection,
      final PlanId planId
  ) throws SQLException {
    try (
        final var getSimulationAction = new GetSimulationAction(connection);
        final var getSimulationTemplateAction = new GetSimulationTemplateAction(connection);
    ) {
      final var arguments = new HashMap<String, SerializedValue> ();
      final var simRecord$ = getSimulationAction.get(planId.id());

      if (simRecord$.isPresent()) {
        final var simRecord = simRecord$.get();
        final var templateId$ = simRecord.simulationTemplateId();

        // Apply template arguments followed by simulation arguments.
        // Overwriting of template arguments with sim. arguments is intentional here,
        // and the resulting set of arguments is assumed to be complete
        if (templateId$.isPresent()) {
          getSimulationTemplateAction.get(templateId$.get()).ifPresent(simTemplateRecord -> {
            arguments.putAll(simTemplateRecord.arguments());
          });
        }
        arguments.putAll(simRecord.arguments());
      }

      return arguments;
    }
  }

  @Override
  public long getPlanRevision(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      return getPlanRecord(connection, planId).revision();
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan revision", ex);
    }
  }

  @Override
  public PostgresPlanRevisionData getPlanRevisionData(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getPlanRevisionDataAction = new GetPlanRevisionDataAction(connection)) {
        return getPlanRevisionDataAction
            .get(planId.id())
            .orElseThrow(() -> new NoSuchPlanException(planId));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan revision data", ex);
    }
  }

  @Override
  public Map<ActivityInstanceId, ActivityInstance> getAllActivitiesInPlan(final PlanId planId)
  throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      return getPlanActivities(connection, planId);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get all activities from plan", ex);
    }
  }

  @Override
  public Map<String, Constraint> getAllConstraintsInPlan(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getPlanConstraintsAction = new GetPlanConstraintsAction(connection)) {
        return getPlanConstraintsAction
            .get(planId.id())
            .orElseThrow(() -> new NoSuchPlanException(planId))
            .stream()
            .collect(Collectors.toMap(
                ConstraintRecord::name,
                r -> new Constraint(
                    r.name(),
                    r.summary(),
                    r.description(),
                    r.definition())));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to retrieve constraints for plan with id `%s`".formatted(planId), ex);
    }
  }

  @Override
  public long addExternalDataset(
      final PlanId planId,
      final Timestamp datasetStart,
      final ProfileSet profileSet
  ) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      final var plan = getPlanRecord(connection, planId);
      final var planDataset = createPlanDataset(connection, planId, plan.startTime(), datasetStart);
      ProfileRepository.postResourceProfiles(
          connection,
          planDataset.datasetId(),
          profileSet,
          datasetStart
      );

      return planDataset.datasetId();
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to add external dataset to plan with id `%s`".formatted(planId), ex);
    }
  }

  private PlanRecord getPlanRecord(
      final Connection connection,
      final PlanId planId
  ) throws SQLException, NoSuchPlanException {
    try (final var getPlanAction = new GetPlanAction(connection)) {
      return getPlanAction
          .get(planId.id())
          .orElseThrow(() -> new NoSuchPlanException(planId));
    }
  }

  private Map<ActivityInstanceId, ActivityInstance> getPlanActivities(
      final Connection connection,
      final PlanId planId
  ) throws SQLException, NoSuchPlanException {
    try (
        final var getPlanAction = new GetPlanAction(connection);
        final var getActivitiesAction = new GetActivityDirectivesAction(connection)
    ) {
      final var planStart = getPlanAction
          .get(planId.id())
          .orElseThrow(() -> new NoSuchPlanException(planId))
          .startTime();

      return getActivitiesAction
          .get(planId.id())
          .stream()
          .collect(Collectors.toMap(
              a -> new ActivityInstanceId(a.id()),
              a -> new ActivityInstance(
                  a.type(),
                  planStart.plusMicros(a.startOffsetInMicros()),
                  a.arguments())));
    }
  }

  // TODO: This functionality is not required for the use-case
  //       we are addressing at the time of creation, but it
  //       will be necessary for our future use-cases of associating
  //       multiple plans with an external dataset. At that time,
  //       this function should be lifted to the PlanRepository interface
  //       and hooked up to the merlin bindings
  private static void useExternalDataset(
      final Connection connection,
      final PlanId planId,
      final long datasetId,
      final Timestamp planStart
  ) throws SQLException {
    associatePlanWithDataset(connection, planId, datasetId, planStart);
  }

  private static long toMissionModelId(final String modelId)
  throws NoSuchMissionModelException
  {
    try {
      return Long.parseLong(modelId, 10);
    } catch (final NumberFormatException ex) {
      throw new NoSuchMissionModelException();
    }
  }

  private static PlanDatasetRecord createPlanDataset(
      final Connection connection,
      final PlanId planId,
      final Timestamp planStart,
      final Timestamp datasetStart
  ) throws SQLException {
    try (final var createPlanDatasetAction = new CreatePlanDatasetAction(connection);
         final var createProfileSegmentPartitionAction = new CreateProfileSegmentPartitionAction(connection)) {
      final var pdr = createPlanDatasetAction.apply(planId.id(), planStart, datasetStart);
      createProfileSegmentPartitionAction.apply(pdr.datasetId());
      return pdr;
    }
  }

  private static PlanDatasetRecord associatePlanWithDataset(
      final Connection connection,
      final PlanId planId,
      final long datasetId,
      final Timestamp planStart
  ) throws SQLException {
    try (final var associatePlanDatasetAction = new AssociatePlanDatasetAction(connection)) {
      return associatePlanDatasetAction.apply(planId.id(), datasetId, planStart);
    }
  }

  /*package-local*/ static Map<ActivityInstanceId, ActivityInstance> parseActivitiesJson(final String json, final Timestamp planStartTime) {
    try {
      final var activityRowP =
          productP
              .field("id", activityInstanceIdP)
              .field("start_offset_in_micros", longP)
              .field("type", stringP)
              .field("arguments", mapP(serializedValueP))
              .map(Iso.of(
                  untuple((actId, startOffsetInMicros, type, arguments) ->
                              tuple(
                                  actId,
                                  new ActivityInstance(type,
                                                       planStartTime.plusMicros(startOffsetInMicros),
                                                       arguments))),
                  untuple((ActivityInstanceId actId, ActivityInstance $) ->
                              tuple(actId, planStartTime.microsUntil($.startTimestamp), $.type, $.arguments))));

      final var activities = new HashMap<ActivityInstanceId, ActivityInstance>();
      for (final var entry : parseJson(json, listP(activityRowP))) {
        activities.put(entry.getKey(), entry.getValue());
      }

      return activities;
    } catch (final InvalidJsonException ex) {
      throw new UnexpectedJsonException("The JSON returned from the database has an unexpected structure", ex);
    } catch (final InvalidEntityException ex) {
      throw new UnexpectedJsonException("The JSON returned from the database is syntactically invalid", ex);
    }
  }

  private static <T> T parseJson(final String subject, final JsonParser<T> parser)
  throws InvalidJsonException, InvalidEntityException
  {
    try {
      final var requestJson = Json.createReader(new StringReader(subject)).readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow($ -> new InvalidEntityException(List.of($)));
    } catch (final JsonParsingException ex) {
      throw new InvalidJsonException(ex);
    }
  }

  /*package-local*/ static Map<String, SerializedValue> parseActivityArgumentsJson(final String json) {
    try {
      return parseJson(json, mapP(serializedValueP));
    } catch (final InvalidJsonException ex) {
      throw new UnexpectedJsonException("The JSON returned from the database has an unexpected structure", ex);
    } catch (final InvalidEntityException ex) {
      throw new UnexpectedJsonException("The JSON returned from the database is syntactically invalid", ex);
    }
  }

  private static final class PostgresPlanTransaction implements PlanTransaction {
    private final DataSource dataSource;
    private final PlanId planId;

    private Optional<String> name = Optional.empty();
    private Optional<Timestamp> startTime = Optional.empty();
    private Optional<Timestamp> endTime = Optional.empty();

    public PostgresPlanTransaction(final DataSource dataSource, final PlanId planId) {
      this.dataSource = dataSource;
      this.planId = planId;
    }

    @Override
    public void commit() throws NoSuchPlanException {
      try (final var connection = this.dataSource.getConnection()) {
        try (final var updatePlanAction = new UpdatePlanAction(connection)) {
          updatePlanAction.apply(
              this.planId.id(),
              this.name.orElse(null),
              this.startTime.orElse(null),
              this.endTime.orElse(null));
        }
      } catch (final FailedUpdateException ex) {
        throw new NoSuchPlanException(this.planId);
      } catch (final SQLException ex) {
        throw new DatabaseException("Failed to update a plan", ex);
      }
    }

    @Override
    public PlanTransaction setName(final String name) {
      this.name = Optional.of(name);
      return this;
    }

    @Override
    public PlanTransaction setStartTimestamp(final Timestamp timestamp) {
      this.startTime = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setEndTimestamp(final Timestamp timestamp) {
      this.endTime = Optional.of(timestamp);
      return this;
    }

    @Override
    public PlanTransaction setConfiguration(final Map<String, SerializedValue> configuration) {
      return this;
    }
  }
}
