package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PostgresMissionModelRepository implements MissionModelRepository {
  private final Path missionModelsPath = Path.of("merlin_file_store").toAbsolutePath();
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
  public String createMissionModel(final MissionModelJar missionModelJar) {
    // TODO: Separate JAR upload from mission model registration.
    //   A client should be able to upload any file, then reference that file for any purpose,
    //   be it as a mission model source or as an input file argument for activities/configuration.
    try (
        final var connection = this.dataSource.getConnection();
        final var transactionContext = new TransactionContext(connection);
        final var filesContext = new CreatedFilesContext()
    ) {
      // 1. Add the file to the filesystem.
      final Path jarPath;
      {
        jarPath = getUnusedFilename(
            this.missionModelsPath,
            Optional
                .ofNullable(missionModelJar.path.getFileName())
                .map(Path::toString)
                .orElse("missionModel"));

        try {
          Files.copy(missionModelJar.path, jarPath);
        } catch (final IOException ex) {
          throw new CreateUploadedFileException(missionModelJar.path, jarPath, ex);
        }

        // Delete the file from the filesystem if later steps fail.
        filesContext.addPath(jarPath);
      }

      // 2. Register the file in the Postgres store.
      final long modelId;
      try (
          final var createModelAction = new CreateModelAction(connection);
          final var createUploadedFileAction = new CreateUploadedFileAction(connection)
      ) {
        final long jarId = createUploadedFileAction.apply(
            missionModelJar.path.getFileName().getFileName().toString(),
            this.missionModelsPath.relativize(jarPath).normalize());

        modelId = createModelAction.apply(
            missionModelJar.name,
            missionModelJar.version,
            missionModelJar.mission,
            missionModelJar.owner,
            jarId);
      }

      transactionContext.commit();
      filesContext.commit();

      return Long.toString(modelId);
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to register a mission model", ex);
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
  public void deleteMissionModel(final String missionModelId) throws NoSuchMissionModelException  {
    // TODO: Separate JAR upload from mission model registration.
    //   A client should be able to upload any file, then reference that file for any purpose,
    //   be it as a mission model source or as an input file argument for activities/configuration.
    try (final var connection = this.dataSource.getConnection()) {
      try (
          final var getModelAction = new GetModelAction(connection);
          final var deleteModelAction = new DeleteModelAction(connection)
      ) {
        final var jarPath = getModelAction
            .get(toMissionModelId(missionModelId))
            .orElseThrow(NoSuchMissionModelException::new)
            .path();
        deleteModelAction.apply(toMissionModelId(missionModelId));

        try {
          Files.delete(jarPath);
        } catch (final IOException ex) {
          throw new DeleteUploadedFileException(jarPath, ex);
        }
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete mission model with id `%s`".formatted(missionModelId), ex);
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

  private static Path getUnusedFilename(final Path base, final String preferredName) {
    var path = base.resolve(preferredName + ".jar");
    for (int i = 0; Files.exists(path); ++i) {
      path = base.resolve(preferredName + "_" + i + ".jar");
    }

    return path;
  }
}
