package gov.nasa.jpl.aerie.scheduler.worker.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResultsInterface;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.GlobalConstraint;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingCondition;
import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.ResultsProtocolFailure;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.http.ResponseSerializers;
import gov.nasa.jpl.aerie.scheduler.server.models.DatasetId;
import gov.nasa.jpl.aerie.scheduler.server.models.ExternalProfiles;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalRecord;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalSource;
import gov.nasa.jpl.aerie.scheduler.server.models.MerlinPlan;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanMetadata;
import gov.nasa.jpl.aerie.scheduler.server.models.ResourceType;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingCompilationError;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.GoalBuilder;
import gov.nasa.jpl.aerie.scheduler.server.services.MerlinService;
import gov.nasa.jpl.aerie.scheduler.server.services.MerlinServiceException;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleRequest;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;
import gov.nasa.jpl.aerie.scheduler.server.services.SchedulerAgent;
import gov.nasa.jpl.aerie.scheduler.server.services.SpecificationService;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.apache.commons.lang3.tuple.Pair;

/**
 * agent that handles posed scheduling requests by blocking the requester thread until scheduling is complete
 *
 * @param merlinService interface for querying plan and mission model details from merlin
 * @param modelJarsDir path to parent directory for mission model jars (interim backdoor jar file access)
 * @param goalsJarPath path to jar file to load scheduling goals from (interim solution for user input goals)
 * @param outputMode how the scheduling output should be returned to aerie (eg overwrite or new container)
 */
//TODO: will eventually need scheduling goal service arg to pull goals from scheduler's own data store
public record SynchronousSchedulerAgent(
    SpecificationService specificationService,
    MerlinService.OwnerRole merlinService,
    Path modelJarsDir,
    Path goalsJarPath,
    PlanOutputMode outputMode,
    SchedulingDSLCompilationService schedulingDSLCompilationService,
    Map<Pair<PlanId, PlanningHorizon>, SimulationFacade> simulationFacades,
    boolean useResourceTracker
)
    implements SchedulerAgent
{
  public SynchronousSchedulerAgent {
    Objects.requireNonNull(specificationService);
    Objects.requireNonNull(merlinService);
    Objects.requireNonNull(modelJarsDir);
    Objects.requireNonNull(goalsJarPath);
    Objects.requireNonNull(outputMode);
    Objects.requireNonNull(schedulingDSLCompilationService);
    Objects.requireNonNull(simulationFacades);
  }

  public SynchronousSchedulerAgent(
      SpecificationService specificationService,
      MerlinService.OwnerRole merlinService,
      Path modelJarsDir,
      Path goalsJarPath,
      PlanOutputMode outputMode,
      SchedulingDSLCompilationService schedulingDSLCompilationService,
      boolean useResourceTracker) {
    this(specificationService, merlinService, modelJarsDir, goalsJarPath, outputMode,
         schedulingDSLCompilationService, new HashMap<>(), useResourceTracker);
  }

  /**
   * {@inheritDoc}
   *
   * consumes any ResultsProtocolFailure exception generated by the scheduling process and writes its message as a
   * failure reason to the given output port (eg aerie could not be reached, mission model could not be loaded from jar
   * file, requested plan revision has changed in the database, scheduler could not find a solution, etc)
   *
   * any remaining exceptions passed upward represent fatal service configuration problems
   */
  @Override
  public void schedule(
      final ScheduleRequest request,
      final ResultsProtocol.WriterRole writer,
      final Supplier<Boolean> canceledListener
  ) {
    try {
      //confirm requested plan to schedule from/into still exists at targeted version (request could be stale)
      //TODO: maybe some kind of high level db transaction wrapping entire read/update of target plan revision

      final var specification = specificationService.getSpecification(request.specificationId());
      //TODO: consider caching planMetadata, schedulerMissionModel, Problem, etc. in addition to SimulationFacade
      final var planMetadata = merlinService.getPlanMetadata(specification.planId());
      ensureRequestIsCurrent(request);
      ensurePlanRevisionMatch(specification, planMetadata.planRev());
      //create scheduler problem seeded with initial plan
      final var schedulerMissionModel = loadMissionModel(planMetadata);
      final var planningHorizon = new PlanningHorizon(
          specification.horizonStartTimestamp().toInstant(),
          specification.horizonEndTimestamp().toInstant()
      );
      //TODO: planningHorizon may be different from planMetadata.horizon(); could we reuse a facade with a different horizon?
      try(final var simulationFacade = getSimulationFacade(
          specification.planId(),
          planningHorizon,
          schedulerMissionModel.missionModel(),
          schedulerMissionModel.schedulerModel(),
          canceledListener,
          useResourceTracker)) {
        final var problem = new Problem(
            schedulerMissionModel.missionModel(),
            planningHorizon,
            simulationFacade,
            schedulerMissionModel.schedulerModel()
        );
        final var externalProfiles = loadExternalProfiles(planMetadata.planId());
        final var initialSimulationResults = loadSimulationResults(planMetadata);
        //seed the problem with the initial plan contents
        final var loadedPlanComponents = loadInitialPlan(planMetadata, problem, initialSimulationResults);
        problem.setInitialPlan(loadedPlanComponents.schedulerPlan(), initialSimulationResults);
        problem.setExternalProfile(externalProfiles.realProfiles(), externalProfiles.discreteProfiles());
        //apply constraints/goals to the problem
        final var compiledGlobalSchedulingConditions = new ArrayList<SchedulingCondition>();
        final var failedGlobalSchedulingConditions = new ArrayList<List<SchedulingCompilationError.UserCodeError>>();
        specification.globalSchedulingConditions().forEach($ -> {
          if (!$.enabled()) return;
          final var result = schedulingDSLCompilationService.compileGlobalSchedulingCondition(
              merlinService,
              planMetadata.planId(),
              $.source().source(),
              externalProfiles.resourceTypes());
          if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success<SchedulingDSL.ConditionSpecifier> r) {
            compiledGlobalSchedulingConditions.addAll(conditionBuilder(r.value(), problem));
          } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error<SchedulingDSL.ConditionSpecifier> r) {
            failedGlobalSchedulingConditions.add(r.errors());
          } else {
            throw new Error("Unhandled variant of %s: %s".formatted(
                SchedulingDSLCompilationService.SchedulingDSLCompilationResult.class.getSimpleName(),
                result));
          }
        });

        if (!failedGlobalSchedulingConditions.isEmpty()) {
          writer.failWith(b -> b
              .type("GLOBAL_SCHEDULING_CONDITIONS_FAILED")
              .message("Global scheduling condition%s failed".formatted(failedGlobalSchedulingConditions.size() > 1
                                                                            ? "s"
                                                                            : ""))
              .data(ResponseSerializers.serializeFailedGlobalSchedulingConditions(failedGlobalSchedulingConditions)));
          return;
        }

        compiledGlobalSchedulingConditions.forEach(problem::add);

        final var orderedGoals = new ArrayList<Goal>();
        final var goals = new HashMap<Goal, GoalId>();
        final var compiledGoals = new ArrayList<Pair<GoalRecord, SchedulingDSL.GoalSpecifier>>();
        final var failedGoals = new ArrayList<Pair<GoalId, List<SchedulingCompilationError.UserCodeError>>>();
        for (final var goalRecord : specification.goalsByPriority()) {
          if (!goalRecord.enabled()) continue;
          final var result = compileGoalDefinition(
              merlinService,
              planMetadata.planId(),
              goalRecord.definition(),
              schedulingDSLCompilationService,
              externalProfiles.resourceTypes());
          if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Success<SchedulingDSL.GoalSpecifier> r) {
            compiledGoals.add(Pair.of(goalRecord, r.value()));
          } else if (result instanceof SchedulingDSLCompilationService.SchedulingDSLCompilationResult.Error<SchedulingDSL.GoalSpecifier> r) {
            failedGoals.add(Pair.of(goalRecord.id(), r.errors()));
          } else {
            throw new Error("Unhandled variant of %s: %s".formatted(
                SchedulingDSLCompilationService.SchedulingDSLCompilationResult.class.getSimpleName(),
                result));
          }
        }
        if (!failedGoals.isEmpty()) {
          writer.failWith(b -> b
              .type("SCHEDULING_GOALS_FAILED")
              .message("Scheduling goal%s failed".formatted(failedGoals.size() > 1 ? "s" : ""))
              .data(ResponseSerializers.serializeFailedGoals(failedGoals)));
          return;
        }
        for (final var compiledGoal : compiledGoals) {
          final var goal = GoalBuilder
              .goalOfGoalSpecifier(
                  compiledGoal.getValue(),
                  specification.horizonStartTimestamp(),
                  specification.horizonEndTimestamp(),
                  problem::getActivityType,
                  compiledGoal.getKey().simulateAfter());
          orderedGoals.add(goal);
          goals.put(goal, compiledGoal.getKey().id());
        }
        problem.setGoals(orderedGoals);

        final var scheduler = new PrioritySolver(problem, specification.analysisOnly());
        //run the scheduler to find a solution to the posed problem, if any
        final var solutionPlan = scheduler.getNextSolution().orElseThrow(
            () -> new ResultsProtocolFailure("scheduler returned no solution"));

        final var activityToGoalId = new HashMap<SchedulingActivityDirective, GoalId>();
        for (final var entry : solutionPlan.getEvaluation().getGoalEvaluations().entrySet()) {
          for (final var activity : entry.getValue().getInsertedActivities()) {
            activityToGoalId.put(activity, goals.get(entry.getKey()));
          }
        }
        //store the solution plan back into merlin (and reconfirm no intervening mods!)
        //TODO: make revision confirmation atomic part of plan mutation (plan might have been modified during scheduling!)
        ensurePlanRevisionMatch(specification, getMerlinPlanRev(specification.planId()));
        final var instancesToIds = storeFinalPlan(
            planMetadata,
            loadedPlanComponents.idMap(),
            loadedPlanComponents.merlinPlan(),
            solutionPlan,
            activityToGoalId,
            schedulerMissionModel.schedulerModel()
        );
        final var planMetadataAfterChanges = merlinService.getPlanMetadata(specification.planId());
        final var datasetId = storeSimulationResults(planningHorizon, simulationFacade, planMetadataAfterChanges, instancesToIds);
        //collect results and notify subscribers of success
        final var results = collectResults(solutionPlan, instancesToIds, goals);
        writer.succeedWith(results, datasetId);
      } catch (SchedulingInterruptedException e) {
          writer.reportCanceled(e);
      }
    } catch (final SpecificationLoadException e) {
      writer.failWith(b -> b
          .type("SPECIFICATION_LOAD_EXCEPTION")
          .message(e.toString())
          .data(SchedulingCompilationError.schedulingErrorJsonP.unparse(e.errors))
          .trace(e));
    } catch (final ResultsProtocolFailure e) {
      writer.failWith(b -> b
          .type("RESULTS_PROTOCOL_FAILURE")
          .message(e.toString())
          .trace(e));
    } catch (final NoSuchSpecificationException e) {
      writer.failWith(b -> b
          .type("NO_SUCH_SPECIFICATION")
          .message(e.toString())
          .data(ResponseSerializers.serializeNoSuchSpecificationException(e))
          .trace(e));
    } catch (final NoSuchPlanException e) {
      writer.failWith(b -> b
          .type("NO_SUCH_PLAN")
          .message(e.toString())
          .data(ResponseSerializers.serializeNoSuchPlanException(e))
          .trace(e));
    } catch (final MerlinServiceException e) {
      writer.failWith(b -> b
          .type("PLAN_SERVICE_EXCEPTION")
          .message(e.toString())
          .trace(e));
    } catch (final IOException e) {
      writer.failWith(b -> b
          .type("IO_EXCEPTION")
          .message(e.toString())
          .trace(e));
    }
  }

  private Optional<SimulationResultsInterface> loadSimulationResults(final PlanMetadata planMetadata){
    try {
      return merlinService.getSimulationResults(planMetadata);
    } catch (MerlinServiceException | IOException | InvalidJsonException e) {
      throw new ResultsProtocolFailure(e);
    }
  }

  private SimulationFacade getSimulationFacade(PlanId planId, PlanningHorizon planningHorizon,
                                               final MissionModel<?> missionModel, final SchedulerModel schedulerModel,
                                               final Supplier<Boolean> canceledListener, boolean useResourceTracker) {
    var key = Pair.of(planId, planningHorizon);
    SimulationFacade f = this.simulationFacades.get(key);
    if (f == null) {
      f = new SimulationFacade(planningHorizon, missionModel, schedulerModel, canceledListener, useResourceTracker);
      this.simulationFacades.put(key, f);
    }
    return f;
  }


  private ExternalProfiles loadExternalProfiles(final PlanId planId)
  throws MerlinServiceException, IOException
  {
    return merlinService.getExternalProfiles(planId);
  }

  private Optional<DatasetId> storeSimulationResults(PlanningHorizon planningHorizon, SimulationFacade simulationFacade, PlanMetadata planMetadata,
                                                     final Map<SchedulingActivityDirective, ActivityDirectiveId> schedDirectiveToMerlinId)
      throws MerlinServiceException, IOException, SchedulingInterruptedException {
    if(!simulationFacade.areInitialSimulationResultsStale()) return Optional.empty();
    //finish simulation until end of horizon before posting results
    try {
      simulationFacade.computeSimulationResultsUntil(planningHorizon.getEndAerie());
    } catch (SimulationFacade.SimulationException e) {
      throw new RuntimeException("Error while running simulation before storing simulation results after scheduling", e);
    }
    final var schedID_to_MerlinID =
        schedDirectiveToMerlinId.entrySet().stream()
                                .collect(Collectors.toMap(
                                    (a) -> new SchedulingActivityDirectiveId(a.getKey().id().id()), Map.Entry::getValue));
    final var schedID_to_simID =
        simulationFacade.getActivityIdCorrespondence();
    final var simID_to_MerlinID =
        schedID_to_simID.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getValue,
            (a) -> schedID_to_MerlinID.get(a.getKey())));
    if(simID_to_MerlinID.values().containsAll(schedDirectiveToMerlinId.values()) && schedDirectiveToMerlinId.values().containsAll(simID_to_MerlinID.values())){
      return Optional.of(merlinService.storeSimulationResults(planMetadata,
                                                              simulationFacade.getLatestDriverSimulationResults().get(),
                                                              simID_to_MerlinID));
    } else{
      //schedule in simulation is inconsistent with current state of the plan (user probably disabled simulation for some of the goals)
      return Optional.empty();
    }
  }

  private static SchedulingDSLCompilationService.SchedulingDSLCompilationResult<SchedulingDSL.GoalSpecifier> compileGoalDefinition(
      final MerlinService.ReaderRole merlinService,
      final PlanId planId,
      final GoalSource goalDefinition,
      final SchedulingDSLCompilationService schedulingDSLCompilationService,
      final Collection<ResourceType> additionalResourceTypes)
  {
    return schedulingDSLCompilationService.compileSchedulingGoalDSL(
        merlinService,
        planId,
        goalDefinition.source(),
        additionalResourceTypes
    );
  }

  private void ensurePlanRevisionMatch(final Specification specification, final long actualPlanRev) {
    if (actualPlanRev != specification.planRevision()) {
      throw new ResultsProtocolFailure("plan with id %s at revision %d is no longer at revision %d".formatted(
          specification.planId(), actualPlanRev, specification.planRevision()));
    }
  }
  /**
   * fetch just the current revision number of the target plan from aerie services
   *
   * @param planId identifier of the target plan to load metadata for
   * @return the current revision number of the target plan according to a fresh query
   * @throws ResultsProtocolFailure when the requested plan cannot be found, or aerie could not be reached
   */
  private long getMerlinPlanRev(final PlanId planId)
  throws MerlinServiceException, NoSuchPlanException, IOException
  {
    return merlinService.getPlanRevision(planId);
  }
  /**
   * confirms that specification revision still matches that expected by the scheduling request
   *
   * @param request the original request for scheduling, containing an intended starting specification revision
   * @throws ResultsProtocolFailure when the requested specification revision does not match the actual revision
   */
  private void ensureRequestIsCurrent(final ScheduleRequest request) throws NoSuchSpecificationException {
    final var currentRevisionData = specificationService.getSpecificationRevisionData(request.specificationId());
    if (currentRevisionData.matches(request.specificationRev()) instanceof final RevisionData.MatchResult.Failure failure) {
      throw new ResultsProtocolFailure("schedule specification with id %s is stale: %s".formatted(
          request.specificationId(), failure));
    }
  }

  /**
   * collects the scheduling goals that apply to the current scheduling run on the target plan
   *
   * @param planMetadata details of the plan container whose associated goals should be collected
   * @param mission the mission model that the plan adheres to, possibly associating additional relevant goals
   * @return the list of goals relevant to the target plan
   * @throws ResultsProtocolFailure when the constraints could not be loaded, or the data stores could not be
   *     reached
   */
  private List<GlobalConstraint> loadConstraints(final PlanMetadata planMetadata, final MissionModel<?> mission) {
    //TODO: is the plan and mission model enough to find the relevant constraints? (eg what about sandbox toggling?)
    //TODO: load global constraints from scheduler data store?
    //TODO: load activity type constraints from somewhere (scheduler store? mission model?)
    //TODO: somehow apply user control over which constraints to enforce during scheduling
    return List.of();
  }

  /**
   * load the activity instance content of the specified merlin plan into scheduler-ready objects
   *
   * @param planMetadata metadata of plan container to load from
   * @param problem the problem that the plan adheres to
   * @param initialSimulationResults initial simulation results (optional)
   * @return a plan with all activity instances loaded from the target merlin plan container
   * @throws ResultsProtocolFailure when the requested plan cannot be loaded, or the target plan revision has
   *     changed, or aerie could not be reached
   */
  private PlanComponents loadInitialPlan(
      final PlanMetadata planMetadata,
      final Problem problem,
      final Optional<SimulationResultsInterface> initialSimulationResults) {
    //TODO: maybe paranoid check if plan rev has changed since original metadata?
    try {
      final var merlinPlan =  merlinService.getPlanActivityDirectives(planMetadata, problem);
      final Map<SchedulingActivityDirectiveId, ActivityDirectiveId> schedulingIdToDirectiveId = new HashMap<>();
      final var plan = new PlanInMemory();
      final var activityTypes = problem.getActivityTypes().stream().collect(Collectors.toMap(ActivityType::getName, at -> at));
      for(final var elem : merlinPlan.getActivitiesById().entrySet()){
        final var activity = elem.getValue();
        if(!activityTypes.containsKey(activity.serializedActivity().getTypeName())){
          throw new IllegalArgumentException("Activity type found in JSON object after request to merlin server has "
                                             + "not been found in types extracted from mission model. Probable "
                                             + "inconsistency between mission model used by scheduler server and "
                                             + "merlin server.");
        }
        final var schedulerActType = activityTypes.get(activity.serializedActivity().getTypeName());
        Duration actDuration = null;
        if (schedulerActType.getDurationType() instanceof DurationType.Controllable s) {
          final var serializedDuration = activity.serializedActivity().getArguments().get(s.parameterName());
          if (serializedDuration != null) {
            actDuration = problem.getSchedulerModel().deserializeDuration(serializedDuration);
          }
        } else if (schedulerActType.getDurationType() instanceof DurationType.Fixed fixedDurationType) {
          actDuration = fixedDurationType.duration();
        } else if(schedulerActType.getDurationType() instanceof DurationType.Parametric parametricDurationType) {
          actDuration = parametricDurationType.durationFunction().apply(activity.serializedActivity().getArguments());
        } else if(schedulerActType.getDurationType() instanceof DurationType.Uncontrollable) {
          if(initialSimulationResults.isPresent()){
            for(final var simAct: initialSimulationResults.get().getSimulatedActivities().entrySet()){
              if(simAct.getValue().directiveId().isPresent() &&
                 simAct.getValue().directiveId().get().equals(elem.getKey())){
                actDuration = simAct.getValue().duration();
              }
            }
          }
        } else {
          throw new Error("Unhandled variant of DurationType:" + schedulerActType.getDurationType());
        }
        final var act = SchedulingActivityDirective.fromActivityDirective(elem.getKey(), activity, schedulerActType, actDuration);

        schedulingIdToDirectiveId.put(act.getId(), elem.getKey());
        plan.add(act);
      }
      return new PlanComponents(plan, merlinPlan, schedulingIdToDirectiveId);
    } catch (Exception e) {
      throw new ResultsProtocolFailure(e);
    }
  }

  record PlanComponents(Plan schedulerPlan, MerlinPlan merlinPlan, Map<SchedulingActivityDirectiveId, ActivityDirectiveId> idMap) {}
  record SchedulerMissionModel(MissionModel<?> missionModel, SchedulerModel schedulerModel) {}

  /**
   * creates an instance of the mission model referenced by the specified plan
   *
   * @param plan metadata of the target plan indicating which mission model to load and how to configure the mission
   *     model for that plan data
   * @return instance of the mission model to extract any activity types, constraints, and simulations from
   * @throws ResultsProtocolFailure when the mission model could not be loaded: eg jar file not found, declared
   *     version/name in jar does not match, or aerie filesystem could not be mounted
   */
  private SchedulerMissionModel loadMissionModel(final PlanMetadata plan) {
    try {
      final var missionConfig = SerializedValue.of(plan.modelConfiguration());
      final var modelJarPath = modelJarsDir.resolve(plan.modelPath());
      return new SchedulerMissionModel(
          MissionModelLoader.loadMissionModel(plan.horizon().getStartInstant(), missionConfig, modelJarPath, plan.modelName(), plan.modelVersion()),
          loadSchedulerModelProvider(modelJarPath, plan.modelName(), plan.modelVersion()).getSchedulerModel());
    } catch (MissionModelLoader.MissionModelLoadException | SchedulerModelLoadException e) {
      throw new ResultsProtocolFailure(e);
    }
  }

  public static SchedulerPlugin loadSchedulerModelProvider(final Path path, final String name, final String version)
  throws MissionModelLoader.MissionModelLoadException, SchedulerModelLoadException
  {
    // Look for a MerlinMissionModel implementor in the mission model. For correctness, we're assuming there's
    // only one matching MerlinMissionModel in any given mission model.
    final var className = getImplementingClassName(path, name, version);

    // Construct a ClassLoader with access to classes in the mission model location.
    final var parentClassLoader = Thread.currentThread().getContextClassLoader();
    final URLClassLoader classLoader;
    try {
      classLoader = new URLClassLoader(new URL[] {path.toUri().toURL()}, parentClassLoader);
    } catch (MalformedURLException ex) {
      throw new Error(ex);
    }

    try {
      final var factoryClass$ = classLoader.loadClass(className);
      if (!SchedulerPlugin.class.isAssignableFrom(factoryClass$)) {
        throw new SchedulerModelLoadException(path, name, version);
      }

      // SAFETY: We checked above that SchedulerPlugin is assignable from this type.
      @SuppressWarnings("unchecked")
      final var factoryClass = (Class<? extends SchedulerPlugin>) factoryClass$;

      return factoryClass.getConstructor().newInstance();
    } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException
        | IllegalAccessException | InvocationTargetException ex)
    {
      throw new SchedulerModelLoadException(path, name, version, ex);
    }
  }

  public static String getImplementingClassName(final Path jarPath, final String name, final String version)
  throws SchedulerModelLoadException
  {
    try {
      final var jarFile = new JarFile(jarPath.toFile());
      final var jarEntry = jarFile.getEntry("META-INF/services/" + SchedulerPlugin.class.getCanonicalName());
      if (jarEntry == null) {
        throw new Error("JAR file `" + jarPath + "` did not declare a service called " + SchedulerPlugin.class.getCanonicalName());
      }
      final var inputStream = jarFile.getInputStream(jarEntry);

      final var classPathList = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.toList());

      if (classPathList.size() != 1) {
        throw new SchedulerModelLoadException(jarPath, name, version);
      }

      return classPathList.get(0);
    } catch (final IOException ex) {
      throw new SchedulerModelLoadException(jarPath, name, version, ex);
    }
  }

  public static class SchedulerModelLoadException extends Exception {
    private SchedulerModelLoadException(final Path path, final String name, final String version) {
      this(path, name, version, null);
    }

    private SchedulerModelLoadException(final Path path, final String name, final String version, final Throwable cause) {
      super(
          String.format(
              "No implementation found for `%s` at path `%s` wih name \"%s\" and version \"%s\"",
              SchedulerPlugin.class.getSimpleName(),
              path,
              name,
              version),
          cause);
    }
  }

  /**
   * place the modified activity plan back into the target merlin plan container
   *
   * this will obsolete the locally cached planMetadata since the plan revision will change!
   *
   * @param planMetadata metadata of plan container to store into; outdated after return
   * @param newPlan plan with all activity instances that should be stored to target merlin plan container
   * @throws ResultsProtocolFailure when the plan could not be stored to aerie, the target plan revision has
   *     changed, or aerie could not be reached
   */
  private Map<SchedulingActivityDirective, ActivityDirectiveId> storeFinalPlan(
    final PlanMetadata planMetadata,
    final Map<SchedulingActivityDirectiveId, ActivityDirectiveId> idsFromInitialPlan,
    final MerlinPlan initialPlan,
    final Plan newPlan,
    final Map<SchedulingActivityDirective, GoalId> goalToActivity,
    final SchedulerModel schedulerModel
  ) {
    try {
      switch (this.outputMode) {
        case CreateNewOutputPlan -> {
          return merlinService.createNewPlanWithActivityDirectives(planMetadata, newPlan, goalToActivity, schedulerModel).getValue();
        }
        case UpdateInputPlanWithNewActivities -> {
          return merlinService.updatePlanActivityDirectives(
              planMetadata.planId(),
              idsFromInitialPlan,
              initialPlan,
              newPlan,
              goalToActivity,
              schedulerModel
          );
        }
        default -> throw new IllegalArgumentException("unsupported scheduler output mode " + this.outputMode);
      }
    } catch (Exception e) {
      throw new ResultsProtocolFailure(e);
    }
  }

  public static List<SchedulingCondition> conditionBuilder(SchedulingDSL.ConditionSpecifier conditionSpecifier, Problem problem){
    if(conditionSpecifier instanceof SchedulingDSL.ConditionSpecifier.AndCondition andCondition){
      final var conditions = new ArrayList<SchedulingCondition>();
      andCondition.conditionSpecifiers().forEach( (condition) -> conditions.addAll(conditionBuilder(condition, problem)));
      return conditions;
    } else if(conditionSpecifier instanceof SchedulingDSL.ConditionSpecifier.GlobalSchedulingCondition globalSchedulingCondition){
      return List.of(new SchedulingCondition(
          globalSchedulingCondition.expression(),
          globalSchedulingCondition.activityTypes().stream().map((activityExpression -> problem.getActivityType(activityExpression))).toList()));
    }
    throw new Error("Unhandled variant of %s: %s".formatted(SchedulingDSL.ConditionSpecifier.class.getSimpleName(), conditionSpecifier));
  }

  /**
   * collect output summary of the scheduling run
   *
   * depending on service configuration, this result may be cached and served to later requesters
   *
   * only reports one evaluation's score per goal, even if the goal is scored in multiple evaluations
   *
   * @param plan the target plan after the scheduling run has completed
   * @return summary of the state of the plan after scheduling ran; eg goal success metrics, associated instances, etc
   */
  private ScheduleResults collectResults(final Plan plan, final Map<SchedulingActivityDirective, ActivityDirectiveId> instancesToIds, Map<Goal, GoalId> goalsToIds) {
    Map<GoalId, ScheduleResults.GoalResult> goalResults = new HashMap<>();
      for (var goalEval : plan.getEvaluation().getGoalEvaluations().entrySet()) {
        var goalId = goalsToIds.get(goalEval.getKey());
        //goal could be anonymous, a subgoal of a composite goal for example, and thus have no meaning for results sent back
        final var activitiesById = plan.getActivitiesById();
        if(goalId != null) {
          final var goalResult = new ScheduleResults.GoalResult(
              goalEval
                  .getValue()
                  .getInsertedActivities().stream()
                      .map(activityInstance -> instancesToIds.get(
                          activityInstance.getParentActivity()
                              .map(activitiesById::get)
                              .orElse(activityInstance))
                  ).toList(),
              goalEval
                  .getValue()
                  .getAssociatedActivities().stream()
                      .map(activityInstance -> instancesToIds.get(
                        activityInstance.getParentActivity()
                            .map(activitiesById::get)
                            .orElse(activityInstance))
                    ).toList(),
              goalEval.getValue().getScore() >= 0);
          goalResults.put(goalId, goalResult);
        }
      }
    return new ScheduleResults(goalResults);
  }

}
