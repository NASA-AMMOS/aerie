package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public Map<String, Constraint> getConstraints(final String missionModelId) throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getModelConstraintsAction = new GetModelConstraintsAction(connection)) {
        return getModelConstraintsAction
            .get(toMissionModelId(missionModelId))
            .orElseThrow(NoSuchMissionModelException::new)
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
  public void updateActivityTypes( final String missionModelId, final Map<String, ActivityType> activityTypes)
  throws NoSuchMissionModelException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var createActivityTypeAction = new CreateActivityTypeAction(connection)) {
        final var id = toMissionModelId(missionModelId);
        for (final var activityType : activityTypes.values()) {
          createActivityTypeAction.apply(
              id,
              activityType.name(),
              activityType.parameters(),
              activityType.requiredParameters(),
              activityType.computedAttributesValueSchema());
        }
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(missionModelId), ex);
    }
  }

  @Override
  public void updateActivityDirectiveValidations(String activityDirectiveId, List<ValidationNotice> notices)
  throws NoSuchActivityDirectiveException
  {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var updateActivityDirectiveValidationsAction = new UpdateActivityDirectiveValidationsAction(connection)) {
        final var directiveId = toActivityDirectiveId(activityDirectiveId);
        updateActivityDirectiveValidationsAction.apply(directiveId, notices);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for activity directive with id `%s`".formatted(activityDirectiveId), ex);
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

  private static long toActivityDirectiveId(final String directiveId)
  throws NoSuchActivityDirectiveException
  {
    try {
      return Long.parseLong(directiveId, 10);
    } catch (final NumberFormatException ex) {
      throw new NoSuchActivityDirectiveException();
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

  private static Path getUnusedFilename(final Path base, final String preferredName) {
    var path = base.resolve(preferredName + ".jar");
    for (int i = 0; Files.exists(path); ++i) {
      path = base.resolve(preferredName + "_" + i + ".jar");
    }

    return path;
  }
}
