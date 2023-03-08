package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanDatasetException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import org.apache.commons.lang3.tuple.Pair;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

            plans.put(planId, new Plan(
                record.name(),
                Long.toString(record.missionModelId()),
                record.startTime(),
                record.endTime(),
                activities
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

        return new Plan(
            planRecord.name(),
            Long.toString(planRecord.missionModelId()),
            planRecord.startTime(),
            planRecord.endTime(),
            activities
        );
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan", ex);
    }
  }

  @Override
  public PlanService.SimulationArguments getSimulationArguments(final PlanId planId, final Timestamp startTimestamp, final Duration planDuration) {
    try (final var connection = this.dataSource.getConnection()) {
      return getSimulationArguments(connection, planId.id(), planDuration);
    } catch (SQLException e) {
      throw new DatabaseException("Failed to get simulation arguments", e);
    }
  }

  private PlanService.SimulationArguments getSimulationArguments(
      final Connection connection,
      final long planId,
      final Duration planDuration
  ) throws SQLException {
    try (
        final var getSimulationAction = new GetSimulationAction(connection);
        final var getSimulationTemplateAction = new GetSimulationTemplateAction(connection)
    ) {
      final var arguments = new HashMap<String, SerializedValue> ();
      final var simRecord$ = getSimulationAction.get(planId);

      if (simRecord$.isPresent()) {
        final var simRecord = simRecord$.get();
        final var templateId$ = simRecord.simulationTemplateId();

        // Apply template arguments followed by simulation arguments.
        // Overwriting of template arguments with sim. arguments is intentional here,
        // and the resulting set of arguments is assumed to be complete
        var simulationTemplateRecord = Optional.<SimulationTemplateRecord>empty();
        if (templateId$.isPresent()) {
          simulationTemplateRecord = getSimulationTemplateAction.get(templateId$.get());
          simulationTemplateRecord.ifPresent(simTemplateRecord -> {
            arguments.putAll(simTemplateRecord.arguments());
          });
        }
        arguments.putAll(simRecord.arguments());

        return new PlanService.SimulationArguments(
            simulationTemplateRecord.flatMap(SimulationTemplateRecord::offsetFromPlanStart).orElse(simRecord.offsetFromPlanStart().orElse(Duration.ZERO)),
            simulationTemplateRecord.flatMap(SimulationTemplateRecord::duration).orElse(simRecord.duration().orElse(planDuration)),
            arguments);
      }

      return new PlanService.SimulationArguments(Duration.ZERO, planDuration, arguments);
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
  public Map<ActivityDirectiveId, ActivityDirective> getAllActivitiesInPlan(final PlanId planId)
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
          profileSet
      );

      return planDataset.datasetId();
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to add external dataset to plan with id `%s`".formatted(planId), ex);
    }
  }

  @Override
  public void extendExternalDataset(
      final DatasetId datasetId,
      final ProfileSet profileSet
  ) throws NoSuchPlanDatasetException {
    try (final var connection = this.dataSource.getConnection()) {
      if (!planDatasetExists(connection, datasetId)) {
        throw new NoSuchPlanDatasetException(datasetId);
      }
      ProfileRepository.appendResourceProfiles(
          connection,
          datasetId.id(),
          profileSet
      );
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to extend external dataset with id `%s`".formatted(datasetId), ex);
    }
  }

  private static boolean planDatasetExists(final Connection connection, final DatasetId datasetId) throws SQLException {
    try (final var getPlanDatasetAction = new CheckPlanDatasetExistsAction(connection)) {
      return getPlanDatasetAction.get(datasetId);
    }
  }

  @Override
  public List<Pair<Duration, ProfileSet>> getExternalDatasets(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      final var plan = getPlanRecord(connection, planId);
      final var planDatasets = ProfileRepository.getAllPlanDatasetsForPlan(connection, planId, plan.startTime());
      final var result = new ArrayList<Pair<Duration, ProfileSet>>();
      for (final var planDataset: planDatasets) {
        result.add(Pair.of(
            planDataset.offsetFromPlanStart(),
            ProfileRepository.getProfiles(connection, planDataset.datasetId())
        ));
      }
      return result;
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to get external datasets for plan with id `%s`".formatted(planId), ex);
    }
  }

  @Override
  public Map<String, ValueSchema> getExternalResourceSchemas(final PlanId planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      final var plan = getPlanRecord(connection, planId);
      final var planDatasets = ProfileRepository.getAllPlanDatasetsForPlan(connection, planId, plan.startTime());
      final var result = new HashMap<String, ValueSchema>();
      for (final var planDataset: planDatasets) {
        final var schemas = ProfileRepository.getProfileSchemas(connection, planDataset.datasetId());
        result.putAll(schemas);
      }
      return result;
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to get external resource schemas for plan with id `%s`".formatted(planId), ex
      );
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

  private Map<ActivityDirectiveId, ActivityDirective> getPlanActivities(
      final Connection connection,
      final PlanId planId
  ) throws SQLException, NoSuchPlanException {
    try (
        final var getActivitiesAction = new GetActivityDirectivesAction(connection)
    ) {
      return getActivitiesAction
          .get(planId.id())
          .stream()
          .collect(Collectors.toMap(
              a -> new ActivityDirectiveId(a.id()),
              a -> new ActivityDirective(
                  Duration.of(a.startOffsetInMicros(), Duration.MICROSECONDS),
                  a.type(),
                  a.arguments(),
                  a.anchorId()!=null? new ActivityDirectiveId(a.anchorId()): null,
                  a.anchoredToStart())));
    }
  }

  private static PlanDatasetRecord createPlanDataset(
      final Connection connection,
      final PlanId planId,
      final Timestamp planStart,
      final Timestamp datasetStart
  ) throws SQLException {
    try (final var createPlanDatasetAction = new CreatePlanDatasetAction(connection);
         final var createDatasetPartitionsAction = new CreateDatasetPartitionsAction(connection)) {
      final var pdr = createPlanDatasetAction.apply(planId.id(), planStart, datasetStart);
      createDatasetPartitionsAction.apply(pdr.datasetId());
      return pdr;
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
