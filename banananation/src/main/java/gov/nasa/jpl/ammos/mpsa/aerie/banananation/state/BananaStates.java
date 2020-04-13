package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;

public final class BananaStates {
    public final SettableState<Double> fruitState = new BasicState<>("fruit", 4.0);
    public final SettableState<Double> peelState = new BasicState<>("peel", 4.0);

    public static final DynamicCell<BananaStates> modelRef = DynamicCell.inheritableCell();
    public static BananaStates get() {
        return modelRef.get();
    }
}
