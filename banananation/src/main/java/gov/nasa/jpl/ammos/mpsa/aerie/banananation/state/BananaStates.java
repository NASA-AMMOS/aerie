package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.DynamicCell;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public final class BananaStates {
    public final SettableState<Double> fruitState = new BasicState<>("fruit", 4.0);
    public final SettableState<Double> peelState = new BasicState<>("peel", 4.0);

    public BananaStates(final Instant startTime) {
        this.fruitState.initialize(startTime);
        this.peelState.initialize(startTime);
    }

    public static final DynamicCell<BananaStates> modelRef = DynamicCell.inheritableCell();
    public static BananaStates get() {
        return modelRef.get();
    }
}
