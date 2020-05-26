package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power.InstrumentPower;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.InstrumentDataRateModel;

import java.util.List;

public final class Model {
    /**
     * settable state that tracks the power consumption of the instrument
     *
     * measured in Watts
     *
     * instrument control activities may set this state directly
     *
     * the instrument power consumption is accounted for in the downstream net power
     * rollup and battery state of charge states
     *
     * TODO: instrument power is probably easily calculated from the instrument mode
     */
    public final InstrumentPower instrumentPower_W = new InstrumentPower();
    public final InstrumentDataRateModel instrumentData = new InstrumentDataRateModel("Instrument", 0.0);
    public final InstrumentPower cameraPower = new InstrumentPower();
    public final InstrumentDataRateModel cameraData = new InstrumentDataRateModel("Camera", 0.0);
    public final BinModel dataBin = new BinModel("DataBin", instrumentData);

    public final List<BinModel> allBins = List.of(dataBin);

    public Model(final Instant startTime) {
        this.instrumentPower_W.initialize(startTime);
        this.instrumentData.initialize(startTime);
        this.dataBin.initialize(startTime);
    }
}
