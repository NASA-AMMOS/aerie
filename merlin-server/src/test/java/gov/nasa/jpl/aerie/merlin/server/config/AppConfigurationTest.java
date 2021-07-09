package gov.nasa.jpl.aerie.merlin.server.config;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AppConfigurationTest {
    @Test
    public void testParseProperties() {
        AppConfiguration expected = new AppConfiguration(
            7654,
            JavalinLoggingState.Disabled,
            Optional.empty(),
            new MongoStore(
                URI.create("http://localhost.mongo.test"),
                "mongo_database_test",
                "mongo_plan_collection_test",
                "mongo_activity_collection_test",
                "mongo_adaptation_collection_test",
                "mongo_simulation_results_collection_test"));

        JsonObject config = Json.createObjectBuilder()
                .add("HTTP_PORT", expected.httpPort())
                .add("MONGO_URI", expected.store().uri().toString())
                .add("MONGO_DATABASE", expected.store().database())
                .add("MONGO_PLAN_COLLECTION", expected.store().planCollection())
                .add("MONGO_ACTIVITY_COLLECTION", expected.store().activityCollection())
                .add("MONGO_ADAPTATION_COLLECTION", expected.store().adaptationCollection())
                .add("MONGO_SIMULATION_RESULTS_COLLECTION", expected.store().simulationResultsCollection())
                .build();

        // Parse the JsonObject with parseProperties
        AppConfiguration observed = AppConfiguration.parseProperties(config);

        // Verify the values of each configuration parameter are as expected
        assertThat(observed).isEqualTo(expected);
    }
}
