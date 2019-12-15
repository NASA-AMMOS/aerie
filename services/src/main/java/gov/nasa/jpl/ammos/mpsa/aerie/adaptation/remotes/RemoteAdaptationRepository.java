package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import com.mongodb.client.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

public final class RemoteAdaptationRepository implements AdaptationRepository {
    private final Path ADAPTATION_FILE_PATH = Path.of("adaptation_files").toAbsolutePath();
    private final MongoCollection<Document> adaptationCollection;

    public RemoteAdaptationRepository(
            final URI serverAddress,
            final String databaseName,
            final String adaptationCollectionName
    ) {
        final MongoDatabase database = MongoClients
                .create(serverAddress.toString())
                .getDatabase(databaseName);

        this.adaptationCollection = database.getCollection(adaptationCollectionName);
    }

    public void clear() {
        this.adaptationCollection.drop();
    }

    @Override
    public Stream<Pair<String, AdaptationJar>> getAllAdaptations() {
        final var query = this.adaptationCollection.find();

        return documentStream(query)
                .map(adaptationDocument -> {
                    final String adaptationId = adaptationDocument.getObjectId("_id").toString();
                    final AdaptationJar adaptationJar = adaptationFromDocuments(adaptationDocument);

                    return Pair.of(adaptationId, adaptationJar);
                });
    }

    @Override
    public AdaptationJar getAdaptation(final String id) throws NoSuchAdaptationException {
        final Document adaptationDocument;
        try {
            adaptationDocument = this.adaptationCollection.find(adaptationById(id)).first();
        } catch (IllegalArgumentException e) {
            throw new NoSuchAdaptationException(id);
        }

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException(id);
        }

        return adaptationFromDocuments(adaptationDocument);
    }

    @Override
    public String createAdaptation(final NewAdaptation newAdaptation) {
        final String adaptationId;

        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = newAdaptation.name;
        adaptationJar.version = newAdaptation.version;
        adaptationJar.mission = newAdaptation.mission;
        adaptationJar.owner = newAdaptation.owner;

        // Store Adaptation JAR
        final Path location = FileUtils.getUniqueFilePath(adaptationJar, ADAPTATION_FILE_PATH);
        try {
            Files.createDirectories(location.getParent());
            Files.copy(newAdaptation.path, location);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }
        adaptationJar.path = location;

        {
            final Document adaptationDocument = this.toDocument(adaptationJar);
            this.adaptationCollection.insertOne(adaptationDocument);
            adaptationId = adaptationDocument.getObjectId("_id").toString();
        }

        return adaptationId;
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptationJar = this.getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptationJar.path);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }

        this.adaptationCollection.deleteOne(eq("_id", new ObjectId(adaptationId)));
    }

    private AdaptationJar adaptationFromDocuments(final Document adaptationDocument) {
        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = adaptationDocument.getString("name");
        adaptationJar.version = adaptationDocument.getString("version");
        adaptationJar.mission = adaptationDocument.getString("mission");
        adaptationJar.owner = adaptationDocument.getString("owner");
        adaptationJar.path = Path.of(adaptationDocument.getString("path"));

        return adaptationJar;
    }

    private Document toDocument(final AdaptationJar adaptationJar) {
        final Document adaptationDocument = new Document();
        adaptationDocument.put("name", adaptationJar.name);
        adaptationDocument.put("version", adaptationJar.version);
        adaptationDocument.put("mission", adaptationJar.mission);
        adaptationDocument.put("owner", adaptationJar.owner);
        adaptationDocument.put("path", adaptationJar.path.toString());

        return adaptationDocument;
    }

    private static <T> Stream<T> documentStream(final MongoIterable<T> documents) {
        // Eagerly construct a new iterator so we can close it after the stream is done.
        final MongoCursor<T> cursor = documents.iterator();
        // Wrap the fresh cursor in an Iterable so we can convert it to a Stream.
        final Iterable<T> iterable = () -> cursor;
        // Create a sequential stream that propagates closure to the cursor.
        return StreamSupport
                .stream(iterable.spliterator(), false)
                .onClose(cursor::close);
    }

    private Bson adaptationById(final String adaptationId) throws IllegalArgumentException {
        return eq("_id", new ObjectId(adaptationId));
    }
}
