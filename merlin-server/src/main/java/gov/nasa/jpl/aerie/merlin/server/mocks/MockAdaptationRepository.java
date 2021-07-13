package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoAdaptationRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.merlin.server.utilities.FileUtils.getUniqueFilePath;

public final class MockAdaptationRepository implements AdaptationRepository {
    private final Path ADAPTATION_FILE_PATH;
    private final Map<String, AdaptationJar> adaptations = new HashMap<>();
    private int nextAdaptationId;

    public MockAdaptationRepository() {
        try {
            ADAPTATION_FILE_PATH = Files.createTempDirectory("mock_adaptation_files").toAbsolutePath();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String createAdaptation(final AdaptationJar adaptationJar) {
        // Store Adaptation JAR
        final Path location = getUniqueFilePath(adaptationJar, ADAPTATION_FILE_PATH);
        try {
            Files.copy(adaptationJar.path, location);
        } catch (final IOException e) {
            throw new MongoAdaptationRepository.AdaptationAccessException(adaptationJar.path, e);
        }

        final AdaptationJar newJar = new AdaptationJar(adaptationJar);
        newJar.path = location;

        final String adaptationId = Objects.toString(this.nextAdaptationId++);
        this.adaptations.put(adaptationId, newJar);

        return adaptationId;
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptationJar = getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptationJar.path);
        } catch (final IOException e) {
            throw new MongoAdaptationRepository.AdaptationAccessException(adaptationJar.path, e);
        }

        this.adaptations.remove(adaptationId);
    }

    @Override
    public void replaceConstraints(final String adaptationId, final Map<String, Constraint> constraints)
    throws NoSuchAdaptationException
    {
    }

    @Override
    public void deleteConstraint(final String adaptationId, final String constraintName)
    throws NoSuchAdaptationException
    {
    }

    @Override
    public AdaptationJar getAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptation = Optional
                .ofNullable(this.adaptations.get(adaptationId))
                .orElseThrow(NoSuchAdaptationException::new);

        return new AdaptationJar(adaptation);
    }

    @Override
    public Map<String, Constraint> getConstraints(final String adaptationId) throws NoSuchAdaptationException {
      return Map.of();
    }

    @Override
    public Stream<Pair<String, AdaptationJar>> getAllAdaptations() {
        return this.adaptations
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), new AdaptationJar(entry.getValue())));
    }
}
