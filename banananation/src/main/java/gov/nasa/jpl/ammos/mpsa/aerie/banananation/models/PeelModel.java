package gov.nasa.jpl.ammos.mpsa.aerie.banananation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations.AdaptationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.ResourcesContainer;
import java.util.List;

public class PeelModel implements AdaptationModel {

  private static final int MASHED_BANANA_AMOUNT = 1;
  String direction = "";

  public void setup(List<Parameter> parameters) {
    for (Parameter p : parameters) {
      if (p.getName().equals("direction")) {
        direction = (String) p.getValue();
      }
    }
  }

  public void execute() {

    ResourcesContainer myResources = ResourcesContainer.getInstance();
    Resource peel = myResources.getResourceByName("peel");
    Resource fruit = myResources.getResourceByName("fruit");

    // If we haven't updated the resource previously, check which direction that the banana is
    // being peeled. If it is being peeled from the stem, deduct some fruit since the top of the
    // banana is probably mashed and gross.
    if (!fruit.resourceHistoryHasElements()) {
      if (direction == "fromStem") {
        int amount = (Integer) fruit.getCurrentValue();
        fruit.setValue(amount - MASHED_BANANA_AMOUNT);
      }
    }

    // Peel one section
    int sections = (Integer) peel.getCurrentValue();
    if (sections > 0) {
      peel.setValue(sections--);
    }
  }

}

