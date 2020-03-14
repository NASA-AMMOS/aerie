package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.DownlinkData;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.InitializeBinDataVolume;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.TurnInstrumentOff;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.TurnInstrumentOn;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.defer;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
import static org.assertj.core.api.Assertions.assertThat;

public class DataModelActivitiesTest {
    @Test
    public void initBinsActivity(){
        final var simStart = SimulationInstant.ORIGIN;
        final var states = new OnboardDataModelStates();

        SimulationEngine.simulate(simStart, states, () -> {
            spawn(new InitializeBinDataVolume());
        });

        for (final var x : states.getBinModelList()){
            assertThat(x.getHistory()).hasSize(1);
            assertThat(x.getHistory()).containsValue(0.0);
        }
    }

    @Test
    public void turnInstrumentsOnActivity(){
        final var simStart = SimulationInstant.ORIGIN;
        final var states = new OnboardDataModelStates();

        SimulationEngine.simulate(simStart, states, () -> {
            spawn(new InitializeBinDataVolume());

            TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
            instrumentOn.instrumentName = "instrument 1";
            instrumentOn.instrumentRate = 10.0;
            defer(1, TimeUnit.HOURS, instrumentOn);
        });

        final var binMap = states.getBinByName("Bin 1").getHistory();
        assertThat(binMap).hasSize(2);
        for (final double value : binMap.values()) {
            assertThat(value).isEqualTo(0.0);
        }

        final var instrumentMap = states.getInstrumentByName("instrument 1").getHistory();
        assertThat(instrumentMap).hasSize(1);
        assertThat(instrumentMap).containsValue(10.0);
        assertThat(states.getInstrumentByName("instrument 1").onStatus()).isTrue();
    }

    @Test
    public void turnInstrumentsOffActivity(){
        final var simStart = SimulationInstant.ORIGIN;
        final var states = new OnboardDataModelStates();

        SimulationEngine.simulate(simStart, states, () -> {
            spawn(new InitializeBinDataVolume());

            for (final var model : states.getInstrumentModelList()) {
                TurnInstrumentOff instrumentOff = new TurnInstrumentOff();
                instrumentOff.instrumentName = model.getName();
                defer(1, TimeUnit.MINUTES, instrumentOff);
            }

            TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
            instrumentOn.instrumentName = "instrument 1";
            instrumentOn.instrumentRate = 10.0;
            defer(1, TimeUnit.HOURS, instrumentOn);
        });

        final var instrumentMap = states.getInstrumentByName("instrument 1").getHistory();
        assertThat(instrumentMap).hasSize(2);
        assertThat(instrumentMap).containsValue(0.0);
        assertThat(instrumentMap).containsValue(10.0);

        final var binMap = states.getBinByName("Bin 1").getHistory();
        assertThat(binMap).hasSize(3);
        for (final double x : binMap.values()) {
            assertThat(x).isEqualTo(0.0);
        }
    }

    @Test
    public void downlinkActivity(){
        final var simStart = SimulationInstant.ORIGIN;
        final var states = new OnboardDataModelStates();

        SimulationEngine.simulate(simStart, states, () -> {
            spawn(new InitializeBinDataVolume());

            for (final var model : states.getInstrumentModelList()) {
                TurnInstrumentOff instrumentOff = new TurnInstrumentOff();
                instrumentOff.instrumentName = model.getName();
                defer(1, TimeUnit.MINUTES, instrumentOff);
            }

            TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
            instrumentOn.instrumentName = "instrument 1";
            instrumentOn.instrumentRate = 10.0;
            defer(1, TimeUnit.HOURS, instrumentOn);

            DownlinkData downlinkActivity = new DownlinkData();
            downlinkActivity.binID = "Bin 1";
            downlinkActivity.downlinkAll = true;
            defer(5, TimeUnit.HOURS, downlinkActivity);
        });
    }

    @Test
    public void instrumentModel(){
        final var ethemis = new InstrumentModel("E-THEMIS", 0.0);
        final var suda = new InstrumentModel("SUDA", 10.0);
        final var bin1 = new BinModel("bin 1", ethemis);
        final var reason = new InstrumentModel("REASON", 12.0, bin1);

        final var simStart = SimulationInstant.ORIGIN;
        final StateContainer states = () -> List.of(ethemis, suda, bin1, reason);

        SimulationEngine.simulate(simStart, states, () -> {
            assertThat(ethemis.onStatus()).isFalse();
            assertThat(ethemis.get()).isEqualTo(0.0);

            assertThat(suda.onStatus()).isTrue();
            assertThat(suda.get()).isEqualTo(10.0);

            ethemis.setDataProtocol("Spacewire");
            assertThat(ethemis.getDataProtocol()).isEqualTo("Spacewire");

            ethemis.setBin(bin1);
            assertThat(ethemis.getBinName()).isEqualTo("bin 1");

            assertThat(reason.onStatus()).isTrue();
            assertThat(reason.getBinName()).isEqualTo("bin 1");
            assertThat(reason.get()).isEqualTo(12.0);

            reason.setDataProtocol("UART");
            assertThat(reason.getDataProtocol()).isEqualTo("UART");
        });
    }
}




























