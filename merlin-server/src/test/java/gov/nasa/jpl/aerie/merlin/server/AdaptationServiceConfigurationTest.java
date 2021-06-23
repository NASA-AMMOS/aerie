package gov.nasa.jpl.aerie.merlin.server;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptationServiceConfigurationTest {

    @Test
    public void testParseProperties() {
        // Create JsonObject with expected properties
        int http_port = 7654;
        URI mongo_uri = URI.create("http://localhost.mongo.test");
        String mongo_database = "mongo_database_test";
        String mongo_plan_collection = "mongo_plan_collection_test";
        String mongo_activity_collection = "mongo_activity_collection_test";
        String mongo_adaptation_collection = "mongo_adaptation_collection_test";
        String mongo_simulation_results_collection = "mongo_simulation_results_collection_test";

        AppConfiguration expected = AppConfiguration.builder()
                .setHttpPort(http_port)
                .setMongoUri(mongo_uri)
                .setMongoDatabase(mongo_database)
                .setMongoPlanCollection(mongo_plan_collection)
                .setMongoActivityCollection(mongo_activity_collection)
                .setMongoAdaptationCollection(mongo_adaptation_collection)
                .setMongoSimulationResultsCollection(mongo_simulation_results_collection)
                .build();

        JsonObject config = Json.createObjectBuilder()
                .add("HTTP_PORT", http_port)
                .add("MONGO_URI", mongo_uri.toString())
                .add("MONGO_DATABASE", mongo_database)
                .add("MONGO_PLAN_COLLECTION", mongo_plan_collection)
                .add("MONGO_ACTIVITY_COLLECTION", mongo_activity_collection)
                .add("MONGO_ADAPTATION_COLLECTION", mongo_adaptation_collection)
                .add("MONGO_SIMULATION_RESULTS_COLLECTION", mongo_simulation_results_collection)
                .build();

        // Parse the JsonObject with parseProperties
        AppConfiguration observed = AppConfiguration.parseProperties(config);

        // Verify the values of each configuration parameter are as expected
        assertThat(observed).isEqualTo(expected);
    }
}
