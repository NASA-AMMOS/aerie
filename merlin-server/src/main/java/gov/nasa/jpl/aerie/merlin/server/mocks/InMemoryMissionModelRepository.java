package gov.nasa.jpl.aerie.merlin.server.mocks;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
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
    private final Path MISSION_MODEL_FILE_PATH;
    private final Map<String, MissionModelJar> missionModels = new HashMap<>();
    private int nextMissionModelId;

    public InMemoryMissionModelRepository() {
        try {
            MISSION_MODEL_FILE_PATH = Files.createTempDirectory("mock_missionModel_files").toAbsolutePath();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String createMissionModel(final MissionModelJar missionModelJar) {
        // Store MissionModel JAR
        final Path location = getUniqueFilePath(missionModelJar, MISSION_MODEL_FILE_PATH);
        try {
            Files.copy(missionModelJar.path, location);
        } catch (final IOException e) {
            throw new MissionModelAccessException(missionModelJar.path, e);
        }

        final MissionModelJar newJar = new MissionModelJar(missionModelJar);
        newJar.path = location;

        final String missionModelId = Objects.toString(this.nextMissionModelId++);
        this.missionModels.put(missionModelId, newJar);

        return missionModelId;
    }

    @Override
    public void updateModelParameters(final String missionModelId, final List<Parameter> modelParameters)
    {
    }

    @Override
    public void updateActivityTypes(final String missionModelId, final Map<String, ActivityType> activityTypes)
    {
    }

    @Override
    public void updateActivityDirectiveValidations(final ActivityDirectiveId directiveId, final PlanId planId, final Timestamp argumentsModifiedTime, final List<ValidationNotice> notices)
    {
    }

    public void deleteMissionModel(final String missionModelId) throws NoSuchMissionModelException {
        final MissionModelJar missionModelJar = getMissionModel(missionModelId);

        // Delete mission model JAR
        try {
            Files.deleteIfExists(missionModelJar.path);
        } catch (final IOException e) {
            throw new MissionModelAccessException(missionModelJar.path, e);
        }

        this.missionModels.remove(missionModelId);
    }

  @Override
    public MissionModelJar getMissionModel(final String missionModelId) throws NoSuchMissionModelException {
        final MissionModelJar missionModel = Optional
                .ofNullable(this.missionModels.get(missionModelId))
                .orElseThrow(NoSuchMissionModelException::new);

        return new MissionModelJar(missionModel);
    }

    @Override
    public Map<String, Constraint> getConstraints(final String missionModelId){
      return Map.of();
    }

    @Override
    public Map<String, MissionModelJar> getAllMissionModels() {
        return new HashMap<>(this.missionModels);
    }

  @Override
  public Map<String, ActivityType> getActivityTypes(final String missionModelId) {
    return Map.of();
  }
}
