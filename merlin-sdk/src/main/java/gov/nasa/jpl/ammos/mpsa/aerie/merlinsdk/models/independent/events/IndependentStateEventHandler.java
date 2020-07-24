package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

public interface IndependentStateEventHandler<Result> {
    Result add(String stateName, double amount);
    Result set(String stateName, SerializedParameter value);
}
