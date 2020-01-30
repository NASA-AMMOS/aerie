package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

/**
 * non-functional mock of a simulation context that can be passed to activities under test
 *
 * currently throws exception on any call
 * TODO: probably valuable to have a mock that records calls so we can check them in tests
 */
public class MockSimulationContext<StatesT extends StateContainer>
        implements SimulationContext {

    // override all other methods with exceptions to detect units tests that go off the rails
    @Override public Activity<?> callActivity(Activity<?> childActivity) {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public Activity<?> spawnActivity(Activity<?> childActivity) {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public void delay(Duration duration) {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public void waitForChild(Activity<?> childActivity) {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public void waitForAllChildren() {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public void delayUntil(Instant time) {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }
    @Override public Instant now() {
        throw new UnsupportedOperationException( "mock sim context lacks functionality" ); }

}
