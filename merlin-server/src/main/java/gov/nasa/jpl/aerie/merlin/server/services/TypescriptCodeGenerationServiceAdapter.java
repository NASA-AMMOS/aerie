
package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.TypescriptCodeGenerationService;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TypescriptCodeGenerationServiceAdapter implements ConstraintsCodeGenService {
  private final MissionModelService missionModelService;

  public TypescriptCodeGenerationServiceAdapter(final MissionModelService missionModelService) {
    this.missionModelService = missionModelService;
  }

  @Override
  public String generateTypescriptTypesFromMissionModel(final String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return TypescriptCodeGenerationService
        .generateTypescriptTypes(
            activityTypes(missionModelService, missionModelId),
            missionModelResourceTypes(missionModelService, missionModelId));
  }

  @Override
  public String generateTypescriptTypesFromPlan(PlanId planId, final String missionModelId, PlanRepository planRepository)
  throws MissionModelService.NoSuchMissionModelException, NoSuchPlanException
  {
    return TypescriptCodeGenerationService
        .generateTypescriptTypes(
            activityTypes(missionModelService, missionModelId),
            allResourceTypes(missionModelService, missionModelId, planId, planRepository));
  }

  static Map<String, TypescriptCodeGenerationService.ActivityType> activityTypes(final MissionModelService missionModelService, final String modelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return missionModelService
        .getActivityTypes(modelId)
        .entrySet()
        .stream()
        .map(entry -> Map.entry(
            entry.getKey(),
            new TypescriptCodeGenerationService.ActivityType(
                entry
                    .getValue()
                    .parameters()
                    .stream()
                    .map(p -> new TypescriptCodeGenerationService.Parameter(p.name(), p.schema()))
                    .toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static Map<String, ValueSchema> missionModelResourceTypes(final MissionModelService missionModelService, String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return missionModelService.getStatesSchemas(missionModelId);
  }

  static Map<String, ValueSchema> allResourceTypes(final MissionModelService missionModelService, String missionModelId, final PlanId planId, final PlanRepository planRepository)
  throws MissionModelService.NoSuchMissionModelException, NoSuchPlanException
  {
    //get external schemas too!
    final var profiles = planRepository.getExternalProfiles(planId);
    final var schemapairs = missionModelService.getStatesSchemas(missionModelId);

    HashMap<String, ValueSchema> resources = new HashMap<String, ValueSchema>(schemapairs);
    for (var realEntry : profiles.realProfiles().entrySet()) {
      //default real valueschema
      //TODO: change this if more profiles for reals become possible, like splines
      resources.put(realEntry.getKey(),
                    ValueSchema.ofStruct(Map.of(
                        "initial", ValueSchema.REAL,
                        "rate", ValueSchema.REAL
                    )));
    }

    for (var discreteEntry : profiles.discreteProfiles().entrySet()) {
      resources.put(discreteEntry.getKey(),
                    discreteEntry.getValue().getLeft());
    }

    return resources;
  }
}

