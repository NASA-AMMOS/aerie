package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDK;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;
import org.junit.Test;

import static org.junit.Assert.*;


public class AdaptationBuilderTests {

  @Test
  public void testAdaptationBuilderCanSetName() {
    String name = "banananation";
    AdaptationBuilder builder = MerlinSDK.createAdaptation().withName(name);
    Adaptation adaptation = builder.getAdaptation();
    assertTrue(adaptation.getName() == name);
  }

  @Test
  public void testAdaptationBuilderCanSetId() {
    String id = "banananation";
    AdaptationBuilder builder = MerlinSDK.createAdaptation().withId(id);
    Adaptation adaptation = builder.getAdaptation();
    assertTrue(adaptation.getId() == id);
  }

  @Test
  public void testAdaptationBuilderCanSetMission() {
    String mission = "banananation";
    AdaptationBuilder builder = MerlinSDK.createAdaptation().withMission(mission);
    Adaptation adaptation = builder.getAdaptation();
    assertTrue(adaptation.getMission() == mission);
  }

  @Test
  public void testAdaptationBuilderCanSetVersion() {
    String version = "1.0.0";
    AdaptationBuilder builder = MerlinSDK.createAdaptation().withVersion(version);
    Adaptation adaptation = builder.getAdaptation();
    assertTrue(adaptation.getVersion() == version);
  }

}
