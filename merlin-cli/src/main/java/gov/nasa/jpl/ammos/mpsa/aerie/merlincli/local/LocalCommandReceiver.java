package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.MerlinCommandReceiver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidEntityException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.PlanDeserializer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.NotImplementedException;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

public class LocalCommandReceiver implements MerlinCommandReceiver {
  private final Map<String, Schedule> schedules = new HashMap<>();
  private final Map<String, AdaptationJar> adaptations = new HashMap<>();

  @Override
  public void createPlan(String path) {
    if (!Files.isReadable(Path.of(path))) {
      throw new RuntimeException("Path is not readable");
    } else if (!Files.isRegularFile(Path.of(path))) {
      throw new RuntimeException("Path is not a file");
    }

    final PlanDetail plan;
    try {
      JsonValue planJson = Json.createReader(Files.newInputStream(Path.of(path))).readValue();
      plan = PlanDeserializer.deserializePlan(planJson);
    } catch (IOException e) {
      throw new Error("File exists and is readable, but something went wrong while reading it.");
    } catch (InvalidEntityException e) {
      System.err.println(e);
      return;
    }

    String adaptationId = plan.getAdaptationId();
    List<ScheduledActivity> scheduledActivities = new ArrayList<>();
    List<ActivityInstance> plannedActivities = plan.getActivityInstances();
    try {
      for (ActivityInstance instance : plannedActivities) {
        scheduledActivities.add(new ScheduledActivity(instance));
      }
    } catch (ParseException e) {
      System.err.println(e);
      return;
    }

    Schedule schedule = new Schedule(adaptationId, scheduledActivities);

    String basename = Path.of(path).getFileName().toString();
    String name = basename;
    for (int i=1; this.schedules.containsKey(name); i++) {
      name = basename + i;
    }

    this.schedules.put(name, schedule);
  }

  @Override
  public void updatePlanFromFile(String planId, String path) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void updatePlanFromTokens(String planId, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void deletePlan(String planId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void downloadPlan(String planId, String outName) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void appendActivityInstances(String planId, String path) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayActivityInstance(String planId, String activityId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void updateActivityInstance(String planId, String activityId, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void deleteActivityInstance(String planId, String activityId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listPlans() {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public String createAdaptation(Path path, Adaptation adaptation) {
    if (!Files.isReadable(path)) {
      throw new RuntimeException("Path is not readable");
    } else if (!Files.isRegularFile(path)) {
      throw new RuntimeException("Path is not a file");
    } else if (this.adaptations.containsKey(adaptation.getName())) {
      throw new RuntimeException("An adaptation already exists with that name");
    }

    this.adaptations.put(adaptation.getName(), new AdaptationJar(path));
    return adaptation.getName();
  }

  @Override
  public void deleteAdaptation(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayAdaptation(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listAdaptations() {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listActivityTypes(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayActivityType(String adaptationId, String activityType) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void convertApfFile(String input, String output, String dir, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  /**
   * @deprecated This method is used for development scaffolding, and will be removed in the future.
   *   Use the {@code createPlan} method instead.
   */
  @Deprecated
  public void addSchedule(String scheduleName, Schedule schedule) {
    this.schedules.put(scheduleName, schedule);
  }

  @Override
  public void performSimulation(String planId) {
    if (!schedules.containsKey(planId)) throw new RuntimeException("No such plan `" + planId + "`");

    final var schedule = schedules.get(planId);
    final var adaptationJar = adaptations.get(schedule.adaptationId);
    final var adaptation = loadAdaptationProvider(adaptationJar.jarPath).get();
    final var activityMapper = adaptation.getActivityMapper();

    final var simulationJobs = new ArrayList<ActivityJob<?>>();
    for (final var scheduledActivity : schedule.scheduledActivities) {
      final Activity<? extends StateContainer> activity = activityMapper
          .deserializeActivity(scheduledActivity.activity)
          .orElseThrow(() -> new RuntimeException("Unable to instantiate activity"));

      simulationJobs.add(new ActivityJob<>(activity, scheduledActivity.startTime));
    }

    final var simulationStartTime = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
    final var stateContainer = adaptation.createStateModels();
    final var engine = new SimulationEngine(simulationStartTime, simulationJobs, stateContainer);

    engine.simulate();

    final var samples = new HashMap<String, TreeMap<Instant, Object>>();
    for (final var state : stateContainer.getStateList()) {
      samples.put(state.toString(), new TreeMap<>(state.getHistory()));
    }

    System.out.println(samples);
  }

  @SuppressWarnings("rawtypes")
  private static ServiceLoader.Provider<MerlinAdaptation> loadAdaptationProvider(final Path adaptationPath) {
    Objects.requireNonNull(adaptationPath);

    final URL adaptationURL;
    try {
      // Construct a ClassLoader with access to classes in the adaptation location.
      adaptationURL = adaptationPath.toUri().toURL();
    } catch (final MalformedURLException ex) {
      // This exception only happens if there is no URL protocol handler available to represent a Path.
      // This is highly unexpected, and indicates a fundamental problem with the system environment.
      throw new Error(ex);
    }

    final var parentClassLoader = Thread.currentThread().getContextClassLoader();
    final var classLoader = new URLClassLoader(new URL[]{adaptationURL}, parentClassLoader);

    // Look for MerlinAdaptation implementors in the adaptation.
    final var serviceLoader = ServiceLoader.load(MerlinAdaptation.class, classLoader);

    // Return the first we come across. (This may not be deterministic, so for correctness
    // we're assuming there's only one MerlinAdaptation in any given location.
    return serviceLoader
        .stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No implementation found for `" + MerlinAdaptation.class.getSimpleName() + "`"));
    }
}
