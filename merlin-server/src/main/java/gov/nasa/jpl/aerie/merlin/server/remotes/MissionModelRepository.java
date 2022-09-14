package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;

import java.util.List;
import java.util.Map;

public interface MissionModelRepository {
    // Queries
    Map<String, MissionModelJar> getAllMissionModels();
    MissionModelJar getMissionModel(String id) throws NoSuchMissionModelException;
    Map<String, Constraint> getConstraints(String missionModelId) throws NoSuchMissionModelException;
    Map<String, ActivityType> getActivityTypes(String missionModelId) throws NoSuchMissionModelException;

    // Mutations
    void updateModelParameters(String missionModelId, final List<Parameter> modelParameters) throws NoSuchMissionModelException;
    void updateActivityTypes(String missionModelId, final Map<String, ActivityType> activityTypes) throws NoSuchMissionModelException;
    void updateActivityDirectiveValidations(final String activityDirectiveId, final List<ValidationNotice> notices)
    throws NoSuchActivityDirectiveException;

    final class NoSuchMissionModelException extends Exception {}
    final class NoSuchActivityDirectiveException extends Exception {}
}
