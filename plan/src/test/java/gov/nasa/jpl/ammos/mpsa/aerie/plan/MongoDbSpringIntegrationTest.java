package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Verify that data can be saved to MongoDB without a repository or controller
 */
@DataMongoTest
@ExtendWith(SpringExtension.class)
public class MongoDbSpringIntegrationTest {

    @Test
    @DisplayName("given an object to save, when save is called, the object should be persisted")
    public void testMongoDBTemplateSaves(@Autowired MongoTemplate mongoTemplate) {
        DBObject objectToSave = BasicDBObjectBuilder.start().add("key", "value").get();
        mongoTemplate.save(objectToSave, "collection");
        assertThat(mongoTemplate.findAll(DBObject.class, "collection")).extracting("key")
                .containsOnly("value");
    }
}
