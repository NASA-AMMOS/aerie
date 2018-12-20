package gov.nasa.jpl.mpsa.adaptation.activities;

import gov.nasa.jpl.mpsa.activities.ActivityType;
import gov.nasa.jpl.mpsa.activities.Parameter;
import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.adaptation.activities.models.ExampleModel;
import gov.nasa.jpl.mpsa.adaptation.activities.models.WheelModel;

public class MoveWheel1Activity extends ActivityType {

    AdaptationModel wheel1Model = new WheelModel();


}
