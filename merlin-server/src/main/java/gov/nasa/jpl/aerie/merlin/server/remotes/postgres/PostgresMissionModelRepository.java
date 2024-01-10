package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelId;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;
import org.apache.commons.lang3.tuple.Pair;

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
  public Map<MissionModelId, List<ActivityDirectiveForValidation>> getUnvalidatedDirectives() {
    try (final var connection = this.dataSource.getConnection();
         final var unvalidatedDirectivesAction = new GetUnvalidatedDirectivesAction(connection)) {
      return unvalidatedDirectivesAction.get();
    } catch (SQLException ex) {
      throw new DatabaseException("Failed to get unvalidated activity directives", ex);
    }
  }

  @Override
  public void updateDirectiveValidations(List<Pair<ActivityDirectiveForValidation, MissionModelService.BulkArgumentValidationResponse>> updates) {
    try (final var connection = this.dataSource.getConnection();
         final var updateAction = new UpdateActivityDirectiveValidationsAction(connection)) {
      updateAction.apply(updates);
    } catch (SQLException ex) {
      throw new DatabaseException("Failed to update activity directive validations", ex);
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
