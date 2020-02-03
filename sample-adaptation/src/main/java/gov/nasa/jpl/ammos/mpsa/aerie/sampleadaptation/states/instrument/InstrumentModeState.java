package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.DerivedState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.classes.CustomEnums.InstrumentMode;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.InstrumentDataRateModel;

// NOTE: this derived state solution does NOT add changing values to a state history since it has no knowledge of
//       when such changes occur
public class InstrumentModeState extends DerivedState<InstrumentMode> {

    private InstrumentDataRateModel instrument;

    public InstrumentModeState(InstrumentDataRateModel instrument) {
        this.instrument = instrument;
    }

    @Override
    public InstrumentMode get() {
        return instrument.getMode();
    }

}