package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

public class DemoPowerModelTest {
    @Test
    public void testDemo() {
        final var demo = new DemoPowerModel();

        final var simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        SimulationEngine.simulate(simStart, demo.states, () -> {
            //check initial conditions match setup
            assertThat(demo.states.solarPowerState_W.get())
                    .isCloseTo( demo.referenceSolarPower_W, withinPercentage(0.01 ) );
            assertThat(demo.states.instrumentAPowerState_W.get())
                    .isCloseTo( 0.0, withinPercentage(0.01 ) );
            assertThat(demo.states.instrumentBPowerState_W.get())
                    .isCloseTo( 0.0, withinPercentage(0.01 ) );
            assertThat(demo.states.netPowerState_W.get())
                    .isCloseTo( demo.referenceSolarPower_W, withinPercentage(0.01 ) );
            assertThat(demo.states.batteryEnergyState_J.get())
                    .isCloseTo( demo.startBatteryCharge_pct/100.0*demo.startBatteryCapacity_J,
                            withinPercentage(0.01 ) );
            assertThat(demo.states.batterStateOfChargeState_pct.get())
                    .isCloseTo( demo.startBatteryCharge_pct, withinPercentage(0.01 ) );

            //nothing on for 1000s, just solar power accumulation approx 1000W*1000s=+1MJ
            delay(1000, TimeUnit.SECONDS);
            final double battery_pct_t1000
                    = ( demo.startBatteryCharge_pct / 100.0 * demo.startBatteryCapacity_J
                    + 1000.0 * demo.referenceSolarPower_W )
                    / demo.startBatteryCapacity_J * 100.0;
            assertThat(demo.states.batterStateOfChargeState_pct.get())
                    .isCloseTo( battery_pct_t1000, withinPercentage( 1.0 ) );
            //note extra slack since not doing r^2 solar power or battery capacity reduction math

            //then fire up both instruments at t=1000
            spawn(new InstrumentOn<>(demo.instrumentAPower_W, demo.states.instrumentAPowerState_W)).await();
            spawn(new InstrumentOn<>(demo.instrumentBPower_W, demo.states.instrumentBPowerState_W)).await();

            assertThat(demo.states.instrumentAPowerState_W.get())
                    .isCloseTo( demo.instrumentAPower_W, withinPercentage(0.01 ) );
            assertThat(demo.states.instrumentBPowerState_W.get())
                    .isCloseTo( demo.instrumentBPower_W, withinPercentage(0.01 ) );

            //leave them on for 1000s, so net (1000W-800W-500W)*1000s=-0.3MJ
            delay(1000, TimeUnit.SECONDS);
            final double battery_pct_t2000
                    = ( battery_pct_t1000 / 100.0 * demo.startBatteryCapacity_J
                        + 1000.0 * ( demo.referenceSolarPower_W
                                     - demo.instrumentAPower_W - demo.instrumentBPower_W ) )
                      / demo.startBatteryCapacity_J * 100.0;
            assertThat(demo.states.batterStateOfChargeState_pct.get())
                    .isCloseTo( battery_pct_t2000, withinPercentage( 1.0 ) );
            //note extra slack since still not accounting for r^2 solar power or bat cap fade

            //then shut down both instruments at t=2000
            spawn(new InstrumentOff<>(demo.states.instrumentAPowerState_W)).await();
            spawn(new InstrumentOff<>(demo.states.instrumentBPowerState_W)).await();

            assertThat(demo.states.instrumentAPowerState_W.get())
                    .isCloseTo( 0.0, withinPercentage(0.01 ) );
            assertThat(demo.states.instrumentBPowerState_W.get())
                    .isCloseTo( 0.0, withinPercentage(0.01 ) );

            //leave it to charge for a day to hit max battery
            delay(Duration.fromQuantity(1, TimeUnit.DAYS).minus(2000, TimeUnit.SECONDS));
            assertThat(demo.states.batterStateOfChargeState_pct.get())
                    .isCloseTo( 100.0, withinPercentage( 1.0 ) );
        });
    }
}
