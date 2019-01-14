package gov.nasa.jpl.mpsa.activities.operations;

import gov.nasa.jpl.mpsa.activities.Parameter;

import java.util.List;

public interface AdaptationModel {

    public void setup(List<Parameter> parameters);
    public void execute();

}