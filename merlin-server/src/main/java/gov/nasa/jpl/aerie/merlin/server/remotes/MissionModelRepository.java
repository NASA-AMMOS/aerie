package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveForValidation;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelId;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService.BulkArgumentValidationResponse;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public interface MissionModelRepository {
    // Queries
    Map<String, MissionModelJar> getAllMissionModels();
    MissionModelJar getMissionModel(String id) throws NoSuchMissionModelException;
    Map<String, ActivityType> getActivityTypes(String missionModelId) throws NoSuchMissionModelException;

    // Mutations
    void updateModelParameters(String missionModelId, final List<Parameter> modelParameters) throws NoSuchMissionModelException;
    void updateActivityTypes(String missionModelId, final Map<String, ActivityType> activityTypes) throws NoSuchMissionModelException;
    void updateResourceTypes(String missionModelId, final Map<String, Resource<?>> resourceTypes) throws NoSuchMissionModelException;
    Map<MissionModelId, List<ActivityDirectiveForValidation>> getUnvalidatedDirectives();
    void updateDirectiveValidations(List<Pair<ActivityDirectiveForValidation, BulkArgumentValidationResponse>> updates);

    final class NoSuchMissionModelException extends Exception {}
}
