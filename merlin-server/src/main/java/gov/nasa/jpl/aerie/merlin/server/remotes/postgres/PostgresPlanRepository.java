package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.merlin.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository.NoSuchAdaptationException;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.NotImplementedException;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;

public final class PostgresPlanRepository implements PlanRepository {
  private final DataSource dataSource;

  public PostgresPlanRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Map<String, Plan> getAllPlans() {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getAllPlansAction = new GetAllPlansAction(connection)) {
        return getAllPlansAction.get();
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get all plans", ex);
    }
  }

  @Override
  public Plan getPlan(final String id) throws NoSuchPlanException {
    final var planId = toPlanId(id);
    try (final var connection = this.dataSource.getConnection()) {
      try (
          final var getPlanAction = new GetPlanAction(connection);
          final var getSimulationAction = new GetSimulationAction(connection);
          ) {
        final var planRecord = getPlanAction.get(planId);
        final var arguments = getSimulationAction
            .get(planId)
            .map(SimulationRecord::arguments)
            .orElseGet(Map::of);
        return new Plan(
            planRecord.name(),
            Long.toString(planRecord.adaptationId()),
            planRecord.startTime(),
            planRecord.endTime(),
            planRecord.activities(),
            arguments
        );
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan", ex);
    }
  }

  @Override
  public long getPlanRevision(final String id) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getPlanRevisionAction = new GetPlanRevisionAction(connection)) {
        return getPlanRevisionAction.get(toPlanId(id));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get plan revision", ex);
    }
  }

  @Override
  public Map<String, ActivityInstance> getAllActivitiesInPlan(final String planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getActivitiesAction = new GetActivitiesAction(connection)) {
        return getActivitiesAction.get(toPlanId(planId));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to get all activities from plan", ex);
    }
  }

  @Override
  public CreatedPlan createPlan(final NewPlan plan) throws NoSuchAdaptationException, IntegrationFailureException {
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
        final long planId = createPlanAction.apply(
            plan.name,
            toMissionModelId(plan.adaptationId),
            plan.startTimestamp,
            plan.endTimestamp);

        final List<String> activityIds;
        if (plan.activityInstances == null) {
          activityIds = new ArrayList<>();
        } else {
          activityIds = new ArrayList<>(plan.activityInstances.size());

          for (final var activity : plan.activityInstances) {
            final long activityId = createActivityAction.apply(
                planId,
                plan.startTimestamp,
                activity.startTimestamp,
                activity.type);

            for (final var argument : activity.parameters.entrySet()) {
              // Add this argument to the staged batch of arguments.
              setActivityArgumentsAction.add(activityId, argument.getKey(), argument.getValue());
            }

            activityIds.add(Long.toString(activityId));
          }

          // Insert all the accumulated arguments for all activities at once.
          setActivityArgumentsAction.apply();
        }

        // Commit our changes so that they become visible to other agents.
        transactionContext.commit();

        return new CreatedPlan(Long.toString(planId), activityIds);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to create a plan", ex);
    }
  }

  @Override
  public PlanTransaction updatePlan(final String id) throws NoSuchPlanException {
    return new PostgresPlanTransaction(this.dataSource, toPlanId(id));
  }

  @Override
  public void deletePlan(final String id) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public String createActivity(final String planId, final ActivityInstance activity) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public void deleteAllActivities(final String planId) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public Map<String, Constraint> getAllConstraintsInPlan(final String planId) throws NoSuchPlanException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getPlanConstraintsAction = new GetPlanConstraintsAction(connection)) {
        return getPlanConstraintsAction.get(toPlanId(planId));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to retrieve constraints for plan with id `%s`".formatted(planId), ex);
    }
  }

  private static long toPlanId(final String id) throws NoSuchPlanException {
    try {
      return Long.parseLong(id, 10);
    } catch (final NumberFormatException ex) {
      throw new NoSuchPlanException(id);
    }
  }

  private static long toActivityId(final String planId, final String id) throws NoSuchActivityInstanceException {
    try {
      return Long.parseLong(id, 10);
    } catch (final NumberFormatException ex) {
      throw new NoSuchActivityInstanceException(planId, id);
    }
  }

  private static long toMissionModelId(final String modelId)
  throws NoSuchAdaptationException
  {
    try {
      return Long.parseLong(modelId, 10);
    } catch (final NumberFormatException ex) {
      throw new NoSuchAdaptationException();
    }
  }

  /*package-local*/ static Map<String, ActivityInstance> parseActivitiesJson(final String json, final Timestamp planStartTime) {
    try {
      final var activityRowP =
          productP
              .field("id", longP.map(Iso.of($ -> Long.toString($), $ -> Long.parseLong($))))
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
                  untuple((String actId, ActivityInstance $) ->
                              tuple(actId, planStartTime.microsUntil($.startTimestamp), $.type, $.parameters))));

      final var activities = new HashMap<String, ActivityInstance>();
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
    private final long planId;

    private Optional<String> name = Optional.empty();
    private Optional<Timestamp> startTime = Optional.empty();
    private Optional<Timestamp> endTime = Optional.empty();

    public PostgresPlanTransaction(final DataSource dataSource, final long planId) {
      this.dataSource = dataSource;
      this.planId = planId;
    }

    @Override
    public void commit() throws NoSuchPlanException {
      try (final var connection = this.dataSource.getConnection()) {
        try (final var updatePlanAction = new UpdatePlanAction(connection)) {
          updatePlanAction.apply(
              this.planId,
              this.name.orElse(null),
              this.startTime.orElse(null),
              this.endTime.orElse(null));
        }
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
