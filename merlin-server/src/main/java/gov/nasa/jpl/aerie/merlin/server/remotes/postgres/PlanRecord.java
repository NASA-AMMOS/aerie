package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.Map;

public final record PlanRecord(
    long id,
    long revision,
    String name,
    long missionModelId,
    Timestamp startTime,
    Timestamp endTime,
    Map<ActivityInstanceId, ActivityInstance> activities
) {}
