package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.models.PeelModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDK;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ActivityTypeBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.models.FruitModel;

public class Banananation implements MerlinSDKAdaptation {

  public AdaptationBuilder init() {

    // The Builder pattern solves several problems: 
    //
    // 1. Not being able to get parameters via reflection
    // 2. Designating which models to use at runtime
    // 3. Clipper's YAML based activity dictionary
    //
    // It has several benefits in addition to solving those problems:
    //
    // - Requires less boilerplate than separate Class files
    // - May outperform reflection (https://stackoverflow.com/a/19563000)
    // - Is very DSL-like which might be easier to understand

    AdaptationBuilder adaptation = MerlinSDK.createAdaptation()
        .withName("banananation")
        .withId("banananation")
        .withMission("example")
        .withVersion("1.0.0");

    adaptation.createResource()
        .withName("peel")
        .forSubsystem("peel")
        .ofType(Integer.class)
        .withUnits("sections")
        .withMin(2)
        .withMax(4);

    adaptation.createResource()
        .withName("fruit")
        .forSubsystem("fruit")
        .ofType(Integer.class)
        .withUnits("bites")
        .withMin(4)
        .withMax(8);

    ActivityTypeBuilder biteBanana = adaptation.createActivityType()
        .withName("BiteBanana")
        .withModel(new FruitModel());

    biteBanana.createParameter()
        .withName("size")
        .ofType(Integer.class)
        .withValue(1)
        .asReadOnly(false);

    ActivityTypeBuilder peelBanana = adaptation.createActivityType()
        .withName("PeelBanana")
        .withModel(new PeelModel());

    peelBanana.createParameter()
        .withName("direction")
        .ofType(String.class)
        .withValue("fromStem")
        .asReadOnly(false);

    return adaptation;

  }

}

