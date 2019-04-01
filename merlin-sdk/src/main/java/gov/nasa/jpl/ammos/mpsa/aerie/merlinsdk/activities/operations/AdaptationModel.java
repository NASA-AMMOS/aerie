package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;

import java.util.List;

public interface AdaptationModel {
    public void setup(List<Parameter> parameters);
    public void execute();
}