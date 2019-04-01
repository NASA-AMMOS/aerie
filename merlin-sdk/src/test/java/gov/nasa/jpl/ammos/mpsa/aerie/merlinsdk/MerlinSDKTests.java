package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ResourceBuilder;
import java.util.List;

import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

public class MerlinSDKTests {

    @Test
    public void testConstructor() {
        MerlinSDK sdk = new MerlinSDK();
        assertTrue(sdk != null);
    }

    @Test
    public void testCreateEmptyAdaptation() {
        AdaptationBuilder adaptation = MerlinSDK.createAdaptation();
        assertTrue(adaptation != null);
    }

    @Test
    public void testCreateBasicAdaptation() {

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

        adaptation.createActivityType()
            .withName("BiteBanana");

        assertTrue(adaptation != null);
    }

    @Test
    public void testResourceIsUpdatedAfterCreation() {
        AdaptationBuilder adaptation = MerlinSDK.createAdaptation().withName("banananation");

        adaptation.createResource()
            .withName("peel")
            .forSubsystem("peel")
            .ofType(Integer.class)
            .withUnits("sections")
            .withMin(2)
            .withMax(4);

        List<ResourceBuilder> resources = adaptation.getResources();
        Resource resource = resources.get(0).getResource();

        assertTrue(resources.size() == 1);
        assertSame(resource.getSubsystem(), "peel");
    }
}