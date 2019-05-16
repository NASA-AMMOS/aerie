package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;
import spice.basic.CSPICE;

public class Main {

  public static void main(String[] args) {

    MerlinSDKAdaptation banananation = new Banananation();
    AdaptationBuilder builder = banananation.init();
    Adaptation adaptation = builder.getAdaptation();

    System.out.println("adaptation name: " + adaptation.getName());
    System.out.println("speed of light: " + CSPICE.clight());
    builder.getActivivityTypes().forEach((a) -> {
      ActivityType activityType = a.getActivityType();
      System.out.println("activity type: " + activityType.getName());
      a.getParameters().forEach((p) -> {
        System.out.println("  parameter: " + p.getName());
      });
    });

  }

}
