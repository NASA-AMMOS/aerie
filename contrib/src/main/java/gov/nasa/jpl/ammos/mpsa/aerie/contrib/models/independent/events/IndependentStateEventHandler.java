package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

public interface IndependentStateEventHandler<Result> {
    Result add(String stateName, double amount);
    Result set(String stateName, SerializedValue value);
}
