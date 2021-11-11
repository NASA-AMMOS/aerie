package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PostgresAdaptationRepository implements AdaptationRepository {
  private final Path adaptationsPath = Path.of("merlin_file_store").toAbsolutePath();
  private final DataSource dataSource;

  public PostgresAdaptationRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Map<String, AdaptationJar> getAllAdaptations() {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getAllMissionModelsAction = new GetAllModelsAction(connection)) {
        return getAllMissionModelsAction.get();
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to retrieve all mission models", ex);
    }
  }

  @Override
  public AdaptationJar getAdaptation(final String adaptationId) throws NoSuchAdaptationException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getMissionModelAction = new GetModelAction(connection)) {
        return getMissionModelAction.get(toMissionModelId(adaptationId));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to retrieve mission model with id `%s`".formatted(adaptationId), ex);
    }
  }

  @Override
  public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var getModelConstraintsAction = new GetModelConstraintsAction(connection)) {
        return getModelConstraintsAction.get(toMissionModelId(adaptationId));
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to retrieve constraints for mission model with id `%s`".formatted(adaptationId), ex);
    }
  }

  @Override
  public String createAdaptation(final AdaptationJar adaptationJar) {
    // TODO: Separate JAR upload from adaptation registration.
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
            this.adaptationsPath,
            Optional
                .ofNullable(adaptationJar.path.getFileName())
                .map(Path::toString)
                .orElse("adaptation"));

        try {
          Files.copy(adaptationJar.path, jarPath);
        } catch (final IOException ex) {
          throw new CreateUploadedFileException(adaptationJar.path, jarPath, ex);
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
            adaptationJar.path.getFileName().getFileName().toString(),
            this.adaptationsPath.relativize(jarPath).normalize());

        modelId = createModelAction.apply(
            adaptationJar.name,
            adaptationJar.version,
            adaptationJar.mission,
            adaptationJar.owner,
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
  public void updateModelParameters(final String adaptationId, final List<Parameter> modelParameters)
  throws NoSuchAdaptationException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var createModelParametersAction = new CreateModelParametersAction(connection)) {
        final var id = toMissionModelId(adaptationId);
        createModelParametersAction.apply(id, modelParameters);
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(adaptationId), ex);
    }
  }

  @Override
  public void updateActivityTypes(final String adaptationId, final Map<String, ActivityType> activityTypes)
  throws NoSuchAdaptationException {
    try (final var connection = this.dataSource.getConnection()) {
      try (final var createActivityTypeAction = new CreateActivityTypeAction(connection)) {
        final var id = toMissionModelId(adaptationId);
        for (final var activityType : activityTypes.values()) {
          createActivityTypeAction.apply(id, activityType.name(), activityType.parameters(), activityType.requiredParameters());
        }
      }
    } catch (final SQLException ex) {
      throw new DatabaseException(
          "Failed to update derived data for mission model with id `%s`".formatted(adaptationId), ex);
    }
  }

  @Override
  public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException  {
    // TODO: Separate JAR upload from adaptation registration.
    //   A client should be able to upload any file, then reference that file for any purpose,
    //   be it as a mission model source or as an input file argument for activities/configuration.
    try (final var connection = this.dataSource.getConnection()) {
      try (
          final var getModelAction = new GetModelAction(connection);
          final var deleteModelAction = new DeleteModelAction(connection)
      ) {
        final var jarPath = getModelAction.get(toMissionModelId(adaptationId)).path;
        deleteModelAction.apply(toMissionModelId(adaptationId));

        try {
          Files.delete(jarPath);
        } catch (final IOException ex) {
          throw new DeleteUploadedFileException(jarPath, ex);
        }
      }
    } catch (final SQLException ex) {
      throw new DatabaseException("Failed to delete mission model with id `%s`".formatted(adaptationId), ex);
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

  private static Path getUnusedFilename(final Path base, final String preferredName) {
    var path = base.resolve(preferredName + ".jar");
    for (int i = 0; Files.exists(path); ++i) {
      path = base.resolve(preferredName + "_" + i + ".jar");
    }

    return path;
  }
}
