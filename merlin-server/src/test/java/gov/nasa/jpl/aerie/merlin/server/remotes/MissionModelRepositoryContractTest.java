package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.server.mocks.InMemoryMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Note:
//   If the tests fail, please try to update the mission model jar file.
//   See the `jarFixturePath` field below for the expected (CLASSPATH-relative) location.
public abstract class MissionModelRepositoryContractTest {
    private final Path jarFixturePath;
    {
        try {
            jarFixturePath = Path.of(
                this.getClass()
                    .getResource("/gov/nasa/jpl/ammos/mpsa/aerie/foo-missionModel-0.6.0-SNAPSHOT.jar")
                    .toURI());
        } catch (final URISyntaxException ex) {
          throw new Error("Unable to find mission model fixture", ex);
        }
    }

    protected InMemoryMissionModelRepository missionModelRepository = null;

    protected abstract void resetRepository();

    @BeforeEach
    public void resetRepositoryBeforeEachTest() {
        this.resetRepository();
    }

    @Test
    public void testGetMissionModel() throws MissionModelRepository.NoSuchMissionModelException {
        // GIVEN
        final MissionModelJar newMissionModel = createValidMissionModelJar("new-missionModel");
        final String id = this.missionModelRepository.createMissionModel(newMissionModel);

        // WHEN
        final MissionModelJar missionModel = this.missionModelRepository.getMissionModel(id);

        // THEN
        assertThat(missionModel.mission).isEqualTo(newMissionModel.mission);
    }

    @Test
    public void testRetrieveAllMissionModels() {
        // GIVEN
        final String id1 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test1"));
        final String id2 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test2"));
        final String id3 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test3"));

        // WHEN
        final Map<String, MissionModelJar> missionModels = this.missionModelRepository
                .getAllMissionModels();

        // THEN
        assertThat(missionModels.size()).isEqualTo(3);
        assertThat(missionModels.get(id1)).isNotNull();
        assertThat(missionModels.get(id2)).isNotNull();
        assertThat(missionModels.get(id3)).isNotNull();
    }

    @Test
    public void testCanDeleteAllMissionModels() throws MissionModelRepository.NoSuchMissionModelException {
        // GIVEN
        final String id1 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test1"));
        final String id2 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test2"));
        final String id3 = this.missionModelRepository.createMissionModel(createValidMissionModelJar("test3"));

        // WHEN
        this.missionModelRepository.deleteMissionModel(id1);
        this.missionModelRepository.deleteMissionModel(id2);
        this.missionModelRepository.deleteMissionModel(id3);

        // THEN
        assertThat(this.missionModelRepository.getAllMissionModels()).isEmpty();
    }

    protected MissionModelJar createValidMissionModelJar(final String mission) {
        final MissionModelJar missionModel = new MissionModelJar();
        missionModel.name = "foo-missionmodel-0.6.0-SNAPSHOT";
        missionModel.version = "0.0.1";
        missionModel.mission = mission;
        missionModel.owner = "Arthur";
        missionModel.path = this.jarFixturePath;
        return missionModel;
    }
}
