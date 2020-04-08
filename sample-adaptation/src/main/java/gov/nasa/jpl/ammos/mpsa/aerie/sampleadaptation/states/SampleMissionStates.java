package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;

public final class SampleMissionStates {
    private SampleMissionStates() {}

    private static final DynamicCell<Model> modelRef = DynamicCell.inheritableCell();

    public static void useModelsIn(final Model model, final Runnable scope) {
        modelRef.setWithin(model, scope::run);
    }

    public static Model getModel() {
        return modelRef.get();
    }
}
