package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.ExternalProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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
  public CreatedPlan createPlan(final NewPlan plan) throws NoSuchMissionModelException, IntegrationFailureException {
    try (
        final var connection = this.dataSource.getConnection();
        // Rollback the transaction if we throw out of this method.
        final var transactionContext = new TransactionContext(connection);
    ) {
      try (
          final var createPlanAction = new CreatePlanAction(connection);
          final var createActivityAction = new CreateActivityAction(connection);
          final var setActivityArgumentsAction = new CreateActivityArgumentsAction(connection)
      ) {
        final var planId = new PlanId(
            createPlanAction.apply(
                plan.name,
                toMissionModelId(plan.missionModelId),
                plan.startTimestamp,
                plan.endTimestamp));

        final List<ActivityInstanceId> activityIds;
        if (plan.activityInstances == null) {
          activityIds = new ArrayList<>();
        } else {
          activityIds = new ArrayList<>(plan.activityInstances.size());

          for (final var activity : plan.activityInstances) {
            final var activityId = createActivityAction.apply(
                planId,
                plan.startTimestamp,
                activity.startTimestamp,
                activity.type);

            for (final var argument : activity.arguments.entrySet()) {
              // Add this argument to the staged batch of arguments.
              setActivityArgumentsAction.add(activityId, argument.getKey(), argument.getValue());
            }

            activityIds.add(new ActivityInstanceId(activityId));
          }

          // Insert all the accumulated arguments for all activities at once.
          setActivityArgumentsAction.apply();
        }

        // Commit our changes so that they become visible to other agents.
        transactionContext.commit();

        return new CreatedPlan(planId, activityIds);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to create a plan", ex);
    }
  }

  @Override
  public PlanTransaction updatePlan(final PlanId planId) throws NoSuchPlanException {
    return new PostgresPlanTransaction(this.dataSource, planId);
  }

  @Override
  public void deletePlan(final PlanId planId) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public ActivityInstanceId createActivity(final PlanId planId, final ActivityInstance activity) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public void deleteAllActivities(final PlanId planId) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
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
      final Timestamp datasetEnd,
      final ProfileSet profileSet
  ) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      final var plan = getPlanRecord(connection, planId);
      final var planDataset = createPlanDataset(connection, planId, plan.startTime(), datasetStart, datasetEnd);
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

  @Override
  public ExternalProfileSet getExternalProfiles(final PlanId planId) throws NoSuchPlanException
  {


    try (final var connection = this.dataSource.getConnection()) {
      /*
      INPUT: planid, OUTPUT: profiles, potentially returned as a pair
      1. get dataset ids of external profiles, using planid as input
      2a. get all profile ids associated with each dataset id
      2b. get all the segments using said profile ids. the actual data is stored in dynamics, as a Dynamics object
       */

      final var datasetIds = getPlanDataset(connection, planId);

      final var planRecord = getPlanRecord(connection, planId);

      //now, using this window, make use of the method that takes window and plan record stuff to get profiles (including segments)
      Map<String, Pair<Window, List<Pair<Duration, RealDynamics>>>> realProfiles = new HashMap<>(Map.of());
      Map<String, Pair<Window, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>> discreteProfiles = new HashMap<>(Map.of());

      for (long datasetId : datasetIds) {
        final var simulationWindow = new Window(planRecord.startTime(), planRecord.endTime()); //TODO: get window from plan_dataset table; the dataset does not necessarily start at plan start (we desire to use offset_from_plan_start)
        final var pairOfStartEnd = getPlanDatasetStart(connection, planId, datasetId, simulationWindow);
        final var datasetStart = pairOfStartEnd.getLeft();
        final var datasetEnd = pairOfStartEnd.getRight();
        final var window = new Window(datasetStart, datasetEnd);
        final var profiles = ProfileRepository.getProfiles(connection, datasetId, window);

        //TODO: ADD GAPS TO REFLECT SPACES BETWEEN INTERVALS
        //TODO: MAKE DURATION END ONCE LAST PROFILE ENDS, NOT AFTER WHOLE THING ENDS - THIS IS IN LINE 62 OF GETPROFILESEGMENTSACTION.JAVA

        for (var discProf : profiles.discreteProfiles().entrySet()) {
          //checks if the same key, this means its a sparse dataset, then make it a sparse set instead of replacing
          if (!discProf.getValue().getRight().isEmpty()) {
            //if key exists
//            if(discreteProfiles.containsKey(discProf.getKey())) {
//              //grab arraylist for given key
//              var existingIntervals = discreteProfiles.get(discProf.getValue()).getRight().getRight();
//              //append new arraylist to that one
//              existingIntervals.addAll(discProf.getValue().getRight());
//              //make a new pair
//              final var newPair = Pair.of(window, Pair.of(discProf.getValue().getLeft(), existingIntervals));
//              //call put again
//              discreteProfiles.put(discProf.getKey(), newPair);
//            }
//            else { //else, just add the key-value pair like normal
              discreteProfiles.put(discProf.getKey(), Pair.of(window, discProf.getValue())); //just replace, if you post the same resource twice, then simply overwrite it, as everything should be included in a single post!
//            }
          }
        }
        for (var realProf : profiles.realProfiles().entrySet()) {
          //checks if the same key, this means its a sparse dataset, then make it a sparse set instead of replacing
          if(!realProf.getValue().isEmpty()) {
//            //if key exists
//            if(realProfiles.containsKey(realProf.getKey())) {
//              //grab arraylist for given key
//              var existingIntervals = realProfiles.get(realProf.getKey()).getRight();
//              //append new arraylist to that one
//              existingIntervals.addAll(realProf.getValue());
//              //call put again
//              realProfiles.put(realProf.getKey(), Pair.of(window, existingIntervals));
//            }
//            else { //else, just add the key-value pair like normal
              realProfiles.put(realProf.getKey(), Pair.of(window, realProf.getValue())); //just replace, if you post the same resource twice, then simply overwrite it, as everything should be included in a single post!
//            }
          }
        }
      }

      //return the map
      return new ExternalProfileSet(realProfiles, discreteProfiles);

    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to add external dataset to plan with id `%s`".formatted(planId), ex);
    } catch (final Exception e) {
      throw e;
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
        final var getActivitiesAction = new GetActivitiesAction(connection)
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
      final Timestamp datasetStart,
      final Timestamp datasetEnd
  ) throws SQLException {
    try (final var createPlanDatasetAction = new CreatePlanDatasetAction(connection);
          final var createProfileSegmentPartitionAction = new CreateProfileSegmentPartitionAction(connection)) {
      final var pdr = createPlanDatasetAction.apply(planId.id(), planStart, datasetStart, datasetEnd);
      createProfileSegmentPartitionAction.apply(pdr.datasetId());
      return pdr;
    }
  }

  private static List<Long> getPlanDataset(
      final Connection connection,
      final PlanId planId
  ) throws SQLException, NoSuchPlanException {
    try (final var getPlanDatasetAction = new GetPlanDatasetAction(connection)) {
      return getPlanDatasetAction.get(planId.id());
    }
  }

  private static Pair<Timestamp, Timestamp> getPlanDatasetStart(
      final Connection connection,
      final PlanId planId,
      final long dataset_id,
      final Window simulationWindow
  ) throws SQLException, NoSuchPlanException {
    try (final var getPlanDatasetStartEnd = new GetPlanDatasetStartEnd(connection)) {
      return getPlanDatasetStartEnd.
          get(planId.id(), dataset_id, simulationWindow);
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
