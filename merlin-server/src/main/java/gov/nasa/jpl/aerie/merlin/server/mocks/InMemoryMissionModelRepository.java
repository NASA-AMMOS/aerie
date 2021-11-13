package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelAccessException;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.utilities.FileUtils.getUniqueFilePath;

public final class InMemoryMissionModelRepository implements MissionModelRepository {
    private final Path ADAPTATION_FILE_PATH;
    private final Map<String, MissionModelJar> adaptations = new HashMap<>();
    private int nextAdaptationId;

    public InMemoryMissionModelRepository() {
        try {
            ADAPTATION_FILE_PATH = Files.createTempDirectory("mock_adaptation_files").toAbsolutePath();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String createAdaptation(final MissionModelJar adaptationJar) {
        // Store Adaptation JAR
        final Path location = getUniqueFilePath(adaptationJar, ADAPTATION_FILE_PATH);
        try {
            Files.copy(adaptationJar.path, location);
        } catch (final IOException e) {
            throw new MissionModelAccessException(adaptationJar.path, e);
        }

        final MissionModelJar newJar = new MissionModelJar(adaptationJar);
        newJar.path = location;

        final String adaptationId = Objects.toString(this.nextAdaptationId++);
        this.adaptations.put(adaptationId, newJar);

        return adaptationId;
    }

    @Override
    public void updateModelParameters(final String adaptationId, final List<Parameter> modelParameters)
    throws NoSuchAdaptationException
    {
    }

    @Override
    public void updateActivityTypes(final String adaptationId, final Map<String, ActivityType> activityTypes)
    throws NoSuchAdaptationException
    {
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final MissionModelJar adaptationJar = getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptationJar.path);
        } catch (final IOException e) {
            throw new MissionModelAccessException(adaptationJar.path, e);
        }

        this.adaptations.remove(adaptationId);
    }

  @Override
    public MissionModelJar getAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final MissionModelJar adaptation = Optional
                .ofNullable(this.adaptations.get(adaptationId))
                .orElseThrow(NoSuchAdaptationException::new);

        return new MissionModelJar(adaptation);
    }

    @Override
    public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
      return Map.of();
    }

    @Override
    public Map<String, MissionModelJar> getAllAdaptations() {
        return new HashMap<>(this.adaptations);
    }
}
