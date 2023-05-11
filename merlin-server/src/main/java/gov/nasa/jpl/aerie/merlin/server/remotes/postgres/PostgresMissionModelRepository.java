package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.ConstraintType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PostgresMissionModelRepository implements MissionModelRepository {
  private final DataSource dataSource;

  public PostgresMissionModelRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Map<String, MissionModelJar> getAllMissionModels() {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getAllMissionModelsAction = new GetAllModelsAction(connection)) {
        return getAllMissionModelsAction
            .get()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> Long.toString(e.getKey()),
                e -> missionModelRecordToMissionModelJar(e.getValue())));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to retrieve all mission models", ex);
    }
  }

  @Override
  public MissionModelJar getMissionModel(final String missionModelId) throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getMissionModelAction = new GetModelAction(connection)) {
        return getMissionModelAction
            .get(toMissionModelId(missionModelId))
            .map(PostgresMissionModelRepository::missionModelRecordToMissionModelJar)
            .orElseThrow(NoSuchMissionModelException::new);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to retrieve mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public Map<Long, Constraint> getConstraints(final String missionModelId) throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getModelConstraintsAction = new GetModelConstraintsAction(connection)) {
        return getModelConstraintsAction
            .get(toMissionModelId(missionModelId))
            .orElseThrow(NoSuchMissionModelException::new)
            .stream()
            .collect(Collectors.toMap(
                ConstraintRecord::id,
                r -> new Constraint(
                    r.name(),
                    r.description(),
                    r.definition(),
                    ConstraintType.MODEL)));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to retrieve constraints for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public Map<String, ActivityType> getActivityTypes(final String missionModelId) throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getActivityTypesAction = new GetActivityTypesAction(connection)) {
        final var id = toMissionModelId(missionModelId);
        final var result = new HashMap<String, ActivityType>();
        for (final var activityType: getActivityTypesAction.get(id)) {
          result.put(activityType.name(), activityType);
        }
        return result;
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to retrieve activity types for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public void updateModelParameters(final String missionModelId, final List<Parameter> modelParameters)
  throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var createModelParametersAction = new CreateModelParametersAction(connection)) {
        final var id = toMissionModelId(missionModelId);
        createModelParametersAction.apply(id, modelParameters);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public void updateActivityTypes(final String missionModelId, final Map<String, ActivityType> activityTypes)
  throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var insertActivityTypesAction = new InsertActivityTypesAction(connection)) {
        final var id = toMissionModelId(missionModelId);
        insertActivityTypesAction.apply((int) id, activityTypes.values());
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public void updateResourceTypes(final String missionModelId, final Map<String, Resource<?>> resources)
  throws NoSuchMissionModelException {
    final var resourceTypes = resources.entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(
                                           Map.Entry::getKey,
                                           entry -> entry.getValue().getOutputType().getSchema()));

    try (final var connection = this.dataSource.getConnection()) {
      try (final var insertResourceTypesAction = new InsertResourceTypesAction(connection)) {
        final long id = toMissionModelId(missionModelId);
        insertResourceTypesAction.apply((int) id, resourceTypes);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public void updateActivityDirectiveValidations(
      final ActivityDirectiveId directiveId,
      final PlanId planId,
      final Timestamp argumentsModifiedTime,
      final List<ValidationNotice> notices
  )
  {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var updateActivityDirectiveValidationsAction = new UpdateActivityDirectiveValidationsAction(connection)) {
        updateActivityDirectiveValidationsAction.apply(directiveId.id(), planId.id(), argumentsModifiedTime, notices);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for activity directive with id `%d` and plan id '%d'".formatted(directiveId.id(), planId.id()), ex);
    }
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

  private static MissionModelJar missionModelRecordToMissionModelJar(final MissionModelRecord record) {
    final var model = new MissionModelJar();
    model.mission = record.mission();
    model.name = record.name();
    model.version = record.version();
    model.owner = record.owner();
    model.path = record.path();

    return model;
  }
}
