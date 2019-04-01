package gov.nasa.jpl.ammos.mpsa.aerie.banananation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations.AdaptationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.ResourcesContainer;
import java.util.List;

public class FruitModel implements AdaptationModel {

  private int biteSize = 1;

  public void setup(List<Parameter> parameters) {
    for (Parameter p : parameters) {
      if (p.getName().equals("size")) {
        biteSize = (Integer) p.getValue();
      }
    }
  }

  public void execute() {
    ResourcesContainer myResources = ResourcesContainer.getInstance();
    Resource fruit = myResources.getResourceByName("fruit");

    // Eat it
    int amount = (Integer) fruit.getCurrentValue();
    if (amount > 0) {
      fruit.setValue(amount - biteSize);
    }
  }

}

