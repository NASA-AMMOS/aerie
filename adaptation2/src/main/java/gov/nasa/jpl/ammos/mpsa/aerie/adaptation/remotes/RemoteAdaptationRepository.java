package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import com.mongodb.client.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.FileUtils;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.MissingAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;
import static gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.AdaptationLoader.loadActivities;

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
    public Stream<Pair<String, Adaptation>> getAllAdaptations() {
        final var query = this.adaptationCollection.find();

        return documentStream(query)
                .map(adaptationDocument -> {
                    final String adaptationId = adaptationDocument.getObjectId("_id").toString();
                    final Adaptation adaptation = adaptationFromDocuments(adaptationDocument);

                    return Pair.of(adaptationId, adaptation);
                });
    }

    @Override
    public Adaptation getAdaptation(final String id) throws NoSuchAdaptationException {
        final Document adaptationDocument = this.adaptationCollection.find(adaptationById(id)).first();

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException(id);
        }

        return adaptationFromDocuments(adaptationDocument);
    }

    @Override
    public Stream<Pair<String, ActivityType>> getAllActivityTypesInAdaptation(final String adaptationId) throws NoSuchAdaptationException, InvalidAdaptationJARException {
        final Adaptation adaptation = this.getAdaptation(adaptationId);

        final Map<String, ActivityType> activityTypes;
        try {
            activityTypes = loadActivities(adaptation.path);
        } catch (final MissingAdaptationException ex) {
            throw new InvalidAdaptationJARException(adaptation.path, ex);
        }

        return activityTypes
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
    }

    @Override
    public ActivityType getActivityTypeInAdaptation(final String adaptationId, final String activityId) throws NoSuchAdaptationException, NoSuchActivityTypeException, InvalidAdaptationJARException {
        final Adaptation adaptation = this.getAdaptation(adaptationId);

        final Map<String, ActivityType> activityTypes;
        try {
            activityTypes = loadActivities(adaptation.path);
        } catch (final MissingAdaptationException ex) {
            throw new InvalidAdaptationJARException(adaptation.path, ex);
        }

        return Optional
                .ofNullable(activityTypes.get(activityId))
                .orElseThrow(() -> new NoSuchActivityTypeException(adaptationId, activityId));
    }

    @Override
    public Map<String, ParameterSchema> getActivityTypeParameters(final String adaptationId, final String activityId) throws NoSuchAdaptationException, NoSuchActivityTypeException, InvalidAdaptationJARException {
        return this.getActivityTypeInAdaptation(adaptationId, activityId).parameters;
    }

    @Override
    public String createAdaptation(final NewAdaptation newAdaptation) {
        final String adaptationId;

        final Adaptation adaptation = new Adaptation();
        adaptation.name = newAdaptation.name;
        adaptation.version = newAdaptation.version;
        adaptation.mission = newAdaptation.mission;
        adaptation.owner = newAdaptation.owner;

        // Store Adaptation JAR
        final Path location = FileUtils.getUniqueFilePath(adaptation, ADAPTATION_FILE_PATH);
        try {
            Files.createDirectories(location.getParent());
            Files.copy(newAdaptation.path, location);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptation.path, e);
        }
        adaptation.path = location;

        {
            final Document adaptationDocument = this.toDocument(adaptation);
            this.adaptationCollection.insertOne(adaptationDocument);
            adaptationId = adaptationDocument.getObjectId("_id").toString();
        }

        return adaptationId;
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final Adaptation adaptation = this.getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptation.path);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptation.path, e);
        }

        this.adaptationCollection.deleteOne(eq("_id", new ObjectId(adaptationId)));
    }

    private Adaptation adaptationFromDocuments(final Document adaptationDocument) {
        final Adaptation adaptation = new Adaptation();
        adaptation.name = adaptationDocument.getString("name");
        adaptation.version = adaptationDocument.getString("version");
        adaptation.mission = adaptationDocument.getString("mission");
        adaptation.owner = adaptationDocument.getString("owner");
        adaptation.path = Path.of(adaptationDocument.getString("path"));

        return adaptation;
    }

    private Document toDocument(final Adaptation adaptation) {
        final Document adaptationDocument = new Document();
        adaptationDocument.put("name", adaptation.name);
        adaptationDocument.put("version", adaptation.version);
        adaptationDocument.put("mission", adaptation.mission);
        adaptationDocument.put("owner", adaptation.owner);
        adaptationDocument.put("path", adaptation.path.toString());

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

    private Bson adaptationById(final String adaptationId) {
        return eq("_id", new ObjectId(adaptationId));
    }
}
