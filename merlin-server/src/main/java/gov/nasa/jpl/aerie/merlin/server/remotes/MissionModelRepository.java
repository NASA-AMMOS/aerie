package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.List;
import java.util.Map;

public interface MissionModelRepository {
    // Queries
    Map<String, MissionModelJar> getAllMissionModels();
    MissionModelJar getMissionModel(String id) throws NoSuchMissionModelException;
    Map<Long, Constraint> getConstraints(String missionModelId) throws NoSuchMissionModelException;
    Map<String, ActivityType> getActivityTypes(String missionModelId) throws NoSuchMissionModelException;

    // Mutations
    void updateModelParameters(String missionModelId, final List<Parameter> modelParameters, Map<String, String> parameterUnits) throws NoSuchMissionModelException;
    void updateActivityTypes(String missionModelId, final Map<String, ActivityType> activityTypes) throws NoSuchMissionModelException;
    void updateActivityDirectiveValidations(final ActivityDirectiveId directiveId, final PlanId planId, final Timestamp argumentsModifiedTime, final List<ValidationNotice> notices);
    void updateResourceTypes(String missionModelId, final Map<String, Resource<?>> resourceTypes, final Map<String, String> resourceTypeUnits) throws NoSuchMissionModelException;

    final class NoSuchMissionModelException extends Exception {}
}
