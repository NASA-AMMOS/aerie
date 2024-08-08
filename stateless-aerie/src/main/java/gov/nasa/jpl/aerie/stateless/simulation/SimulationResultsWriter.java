package gov.nasa.jpl.aerie.stateless.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
import gov.nasa.jpl.aerie.merlin.driver.engine.EventRecord;
import gov.nasa.jpl.aerie.merlin.driver.resources.ResourceProfile;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.EventGraphFlattener;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;
import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.realDynamicsP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.activityArgumentsP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.simulationArgumentsP;


public class SimulationResultsWriter {
  private final static double SCHEMA_VERSION = 1;

  // Write JSONs with Pretty Printing
  private final static Map<String,String> config = Map.of(JsonGenerator.PRETTY_PRINTING, "");

  private final RecursiveTask<JsonObject> profilesTask;
  private final RecursiveTask<JsonObject> eventsTask;
  private final RecursiveTask<JsonObject> spansTask;
  private final RecursiveTask<JsonObject> simConfigTask;

  private final Plan plan;

  public SimulationResultsWriter(SimulationResults results, Plan plan, ResourceFileStreamer rfs) {
    this.plan = plan;
    this.profilesTask = new RecursiveTask<>() {
      @Override
      protected JsonObject compute() {
        try {
          return buildProfiles(results.realProfiles, results.discreteProfiles, rfs);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
    this.eventsTask = new RecursiveTask<>() {
      @Override
      protected JsonObject compute() {
        return buildEvents(results.events,results.topics);
      }
    };
    this.spansTask = new RecursiveTask<>() {
      @Override
      protected JsonObject compute() {
        return buildSpans(results.simulatedActivities,results.unfinishedActivities, plan.simulationStartTimestamp);
      }
    };
    this.simConfigTask = new RecursiveTask<>() {
      @Override
      protected JsonObject compute() {
        return buildSimConfig(plan);
      }
    };
  }

  private void forkSubTasks() {
    // Fork tasks to build subjsons in parallel
    profilesTask.fork();
    eventsTask.fork();
    spansTask.fork();
    simConfigTask.fork();
  }

  public void writeResults(CanceledListener canceledListener, SimulationExtentConsumer consumer) {
    final var stringWriter = new StringWriter();
    try(final var resultsJsonGenerator = Json.createGeneratorFactory(config).createGenerator(stringWriter)) {
      forkSubTasks();

      // Output the starting information
      writeOpening(resultsJsonGenerator, canceledListener.get(), consumer);
      print(resultsJsonGenerator, stringWriter);

      // Join the forked tasks
      resultsJsonGenerator.write("simulationConfiguration", simConfigTask.join());
      print(resultsJsonGenerator, stringWriter);

      resultsJsonGenerator.write("profiles", profilesTask.join());
      print(resultsJsonGenerator, stringWriter);

      resultsJsonGenerator.write("spans", spansTask.join());
      print(resultsJsonGenerator, stringWriter);

      resultsJsonGenerator.write("events", eventsTask.join());
      resultsJsonGenerator.writeEnd();
      print(resultsJsonGenerator, stringWriter);
    }
  }

  public void writeResults(CanceledListener canceledListener, Path outputFilePath, SimulationExtentConsumer consumer) {
    final var stringWriter = new StringWriter();
    try(final var resultsJsonGenerator = Json.createGeneratorFactory(config).createGenerator(stringWriter);
        final var fileWriter = new FileWriter(outputFilePath.toFile()))
    {
      forkSubTasks();

      // Output the starting information
      writeOpening(resultsJsonGenerator, canceledListener.get(), consumer);
      printFile(resultsJsonGenerator, stringWriter, fileWriter);

      // Join the forked tasks
      resultsJsonGenerator.write("simulationConfiguration", simConfigTask.join());
      printFile(resultsJsonGenerator, stringWriter, fileWriter);

      resultsJsonGenerator.write("profiles", profilesTask.join());
      printFile(resultsJsonGenerator, stringWriter, fileWriter);

      resultsJsonGenerator.write("spans", spansTask.join());
      printFile(resultsJsonGenerator, stringWriter, fileWriter);

      resultsJsonGenerator.write("events", eventsTask.join());
      resultsJsonGenerator.writeEnd();
      printFile(resultsJsonGenerator, stringWriter, fileWriter);

      fileWriter.flush();
      System.out.println("Results written to "+outputFilePath);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write to file: "+outputFilePath, e);
    }
  }

  private void print(JsonGenerator resultsGenerator, StringWriter stringWriter) {
    resultsGenerator.flush();
    System.out.print(stringWriter.toString().trim());
    // StringWriter.flush() does nothing, so we must clear the underlying buffer manually
    stringWriter.getBuffer().setLength(0);
    stringWriter.getBuffer().trimToSize(); // deallocates used buffer memory
  }

  private void printFile(JsonGenerator resultsGenerator, StringWriter stringWriter, FileWriter fileWriter)
  throws IOException {
    resultsGenerator.flush();
    fileWriter.write(stringWriter.toString().trim());
    // StringWriter.flush() does nothing, so we must clear the underlying buffer manually
    stringWriter.getBuffer().setLength(0);
    stringWriter.getBuffer().trimToSize(); // deallocates used buffer memory
  }

  private void writeOpening(JsonGenerator resultsGenerator, boolean canceled, SimulationExtentConsumer consumer) {
    final Timestamp simEndTime = plan.simulationStartTimestamp
        .plusMicros(consumer.getLastAcceptedDuration().in(Duration.MICROSECOND));

    resultsGenerator.writeStartObject();
    resultsGenerator.write("version", SCHEMA_VERSION);
    resultsGenerator.write("simulationStartTime", plan.simulationStartTimestamp.toString());
    resultsGenerator.write("simulationEndTime", simEndTime.toString());

    if (canceled) { resultsGenerator.write("canceled", JsonValue.TRUE); }
    else { resultsGenerator.write("canceled", JsonValue.FALSE); }
  }

  private JsonObject buildProfiles(
      final Map<String, ResourceProfile<RealDynamics>> realProfiles,
      final Map<String, ResourceProfile<SerializedValue>> discreteProfiles,
      final ResourceFileStreamer rfs
  ) throws IOException
  {
    final var realProfileBuilder = Json.createArrayBuilder();
    final var discreteProfileBuilder = Json.createArrayBuilder();

    for(final var e : realProfiles.entrySet()) {
      final var name = e.getKey();
      final var profile = e.getValue();
      final var filepath = Path.of(rfs.getFileName(name));

      // Precompute segments
      final var segmentsBuilder = Json.createArrayBuilder();

      try (final var stream = Files.lines(filepath)) {
        stream.forEach(s -> {
          if (!s.isBlank()) {
            try (final JsonReader jr = Json.createReader(new StringReader(s))) {
              segmentsBuilder.add(jr.readObject());
            }
          }
        });
      }

      // If somehow the file didn't exist and didn't except above, use the resources in the rmgr
      if(!Files.deleteIfExists(filepath)){
        profile.segments().forEach(s -> segmentsBuilder.add(Json.createObjectBuilder()
                                                                .add("extent", s.extent().toString())
                                                                .add("dynamics", realDynamicsP.unparse(s.dynamics()))));
      }

      final var profileBuilder = Json.createObjectBuilder()
                                     .add("name", name)
                                     .add("schema", valueSchemaP.unparse(profile.schema()))
                                     .add("segments", segmentsBuilder);

      // Append to the array builder
      realProfileBuilder.add(profileBuilder);
    }

    for(final var e : discreteProfiles.entrySet()) {
      final var name = e.getKey();
      final var profile = e.getValue();
      final var filepath = Path.of(rfs.getFileName(name));

       // Precompute segments
      final var segmentsBuilder = Json.createArrayBuilder();
      try (final var stream = Files.lines(filepath)) {
        stream.forEach(s -> {
          if (!s.isBlank()) {
            try (final JsonReader jr = Json.createReader(new StringReader(s))) {
              segmentsBuilder.add(jr.readObject());
            }
          }
        });
      }

      // If somehow the file didn't exist and didn't except above, use the resources in the rmgr
      if(!Files.deleteIfExists(filepath)){
        profile.segments().forEach(s -> segmentsBuilder.add(Json.createObjectBuilder()
                                                                .add("extent", s.extent().toString())
                                                                .add("dynamics", serializedValueP.unparse(s.dynamics()))));
      }

      final var profileBuilder = Json.createObjectBuilder()
                                     .add("name",name)
                                     .add("schema", valueSchemaP.unparse(profile.schema()))
                                     .add("segments", segmentsBuilder);

      // Append to the array builder
      discreteProfileBuilder.add(profileBuilder);
    }

    return Json.createObjectBuilder()
               .add("realProfiles", realProfileBuilder)
               .add("discreteProfiles", discreteProfileBuilder)
               .build();
  }

  private JsonObject buildSpans(
      final Map<ActivityInstanceId, ActivityInstance> simulatedActivities,
      final Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities,
      final Timestamp simStartTime
  ) {
    final var simulatedActivitiesBuilder = Json.createArrayBuilder();
    final var unfinishedActivitiesBuilder = Json.createArrayBuilder();

    for(final var e : simulatedActivities.entrySet()) {
      final var id = e.getKey();
      final var act = e.getValue();

      // Precompute complicated fields
      final var childIdsBuilder = Json.createArrayBuilder();
      act.childIds().forEach(ci -> childIdsBuilder.add(ci.id()));

      final var startOffset = Duration.of(simStartTime.microsUntil(new Timestamp(act.start())), Duration.MICROSECOND).toString();
      final var endTime = act.start().plus(act.duration().in(Duration.MICROSECOND), ChronoUnit.MICROS).toString();

      // Build activity's builder
      final var actBuilder = Json.createObjectBuilder().add("id", id.id());

      act.directiveId().ifPresentOrElse(did -> actBuilder.add("directiveId", did.id()),
                                        () -> actBuilder.add("directiveId", JsonValue.NULL));
      if(act.parentId() != null) { actBuilder.add("parentId", act.parentId().id()); }
      else { actBuilder.add("parentId", JsonValue.NULL); }

      actBuilder.add("childIds", childIdsBuilder)
                .add("type", act.type())
                .add("startOffset", startOffset)
                .add("duration", act.duration().toString())
                .add("attributes", serializedValueP.unparse(act.computedAttributes()))
                .add("arguments", activityArgumentsP.unparse(act.arguments()))
                .add("startTime", act.start().toString())
                .add("endTime", endTime);

      // Append to the array builder
      simulatedActivitiesBuilder.add(actBuilder);
    }

    for(final var e : unfinishedActivities.entrySet()) {
      final var id = e.getKey();
      final var act = e.getValue();

      // Precompute complicated fields
      final var childIdsBuilder = Json.createArrayBuilder();
      act.childIds().forEach(ci -> childIdsBuilder.add(ci.id()));

      final var startOffset = Duration.of(simStartTime.microsUntil(new Timestamp(act.start())), Duration.MICROSECOND).toString();

      // Build activity's builder
      final var actBuilder = Json.createObjectBuilder().add("id", id.id());

      act.directiveId().ifPresentOrElse(did -> actBuilder.add("directiveId", did.id()),
                                        () -> actBuilder.add("directiveId", JsonValue.NULL));
      if(act.parentId() != null) { actBuilder.add("parentId", act.parentId().id()); }
      else { actBuilder.add("parentId", JsonValue.NULL); }

      actBuilder.add("childIds", childIdsBuilder)
                .add("type", act.type())
                .add("startOffset", startOffset)
                .add("arguments", activityArgumentsP.unparse(act.arguments()))
                .add("startTime", act.start().toString());

      // Append to the array builder
      unfinishedActivitiesBuilder.add(actBuilder);
    }

    return Json.createObjectBuilder()
               .add("simulatedActivities", simulatedActivitiesBuilder)
               .add("unfinishedActivities", unfinishedActivitiesBuilder)
               .build();
  }

  private JsonObject buildEvents(final Map<Duration, List<EventGraph<EventRecord>>> events, final List<Triple<Integer, String, ValueSchema>> topics ) {
    final var eventArrayBuilder = Json.createArrayBuilder();

    for (final var eventPoint : events.entrySet()) {
      final var realTime = eventPoint.getKey();
      final var transactions = eventPoint.getValue();

      for (int transactionIndex = 0; transactionIndex < transactions.size(); transactionIndex++) {
        final var eventGraph = transactions.get(transactionIndex);
        final var flattenedEventGraph = EventGraphFlattener.flatten(eventGraph);

        for (final Pair<String, EventRecord> entry : flattenedEventGraph) {
          final EventRecord event = entry.getRight();
          final var eventBuilder = Json.createObjectBuilder()
                                    .add("causalTime",entry.getLeft())
                                    .add("realTime",realTime.toString())
                                    .add("transactionIndex",transactionIndex)
                                    .add("value", serializedValueP.unparse(event.value()));

          //grab the topic from the event's topic id
          topics.stream()
                .filter(topic -> topic.getLeft() == event.topicId())
                .findFirst()
                .ifPresent(topic -> eventBuilder.add("topic", Json.createObjectBuilder()
                                                                  .add("name",topic.getMiddle())
                                                                  .add("valueSchema", valueSchemaP.unparse(topic.getRight()))));

          // optional span id
          event.spanId().ifPresentOrElse(spanId -> eventBuilder.add("spanId", spanId),
                                         () -> eventBuilder.add("spanId", JsonValue.NULL));
          eventArrayBuilder.add(eventBuilder);
        }
      }
    }

    return Json.createObjectBuilder()
               .add("event",eventArrayBuilder.build())
               .build();
  }

  private JsonObject buildSimConfig(final Plan plan) {
    return Json.createObjectBuilder()
               .add("startTime", plan.simulationStartTimestamp.toString())
               .add("endTime",plan.simulationEndTimestamp.toString())
               .add("arguments", simulationArgumentsP.unparse(plan.configuration))
               .build();
  }

}

/*
Json Schema for Sim results:

{
version: 1.0
simulationStartTime: Timestamp (2024-07-01T00:00:00Z)
simulationEndTime: Timestamp // When the simulation stopped
canceled: boolean

simulationConfiguration: {
   startTime: Timestamp
   endTime: Timestamp
   arguments: {}
}

profiles: {
  realProfiles: [
   {
     name: string
     schema: ValueSchema
     segments: [
       extent: Duration
       dynamics: {}//arbitrary value based on schema
     ]
   }
  ],
  discreteProfiles: [
    {
     name: string
     schema: ValueSchema
     segments: [
       extent: Duration
       dynamics: {}//arbitrary value based on schema
     ]
   }
  ]
}

spans: {
  simulatedActivities: [
    {
      id: int
      directiveId: int | null
      parentId: int | null
      childIds: [int]
      type: String
      startOffset: Duration
      duration: Duration
      attributes: {}
      arguments: {}
      startTime: Timestamp
      endTime: Timestamp
    }
  ],
  unfinishedActivities: [
    {
      id: int
      directiveId: int | null
      parentId: int | null
      childIds: [int]
      type: string
      startOffset: Duration
      arguments: {}
      startTime: Timestamp
    }
  ]
}

events: {
  causalTime : string,
  realTime : Timestamp,
  transactionIndex : int,
  value : {},
  topic: {
    name : string
    valueSchema : {}
  }
  spanId: int,
}
}
 */
