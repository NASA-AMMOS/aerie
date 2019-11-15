package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.*;

/**
 * provides a mocked out simulation engine for use in unit tests
 *
 * all methods except the getCurrentSimulationTime() are non-functional
 *
 * the getCurrentSimulationTime() is overridden to provide a mock time set by the test
 * drivers to any code under test
 */
//TODO: would be better if SimulationEngine was an interface so we could implement just
//     what's necessary directly rather than overriding any inherited core baggage
public class MockTimeSimulationEngine<StatesT extends StateContainer>
        extends SimulationEngine {

    /**
     * creates a new mock simulation engine that is non-functional except to provide
     * time stamps to requesting code under test
     *
     * @param mockTime the initial mock simulation time to report to requestors
     */
    public MockTimeSimulationEngine( Time mockTime ) {
        super(); //default ctor builds a non-functional SimEngine... (why?)
        this.mockSimTime = mockTime;
    }

    /**
     * resets the mock simulation time returned by getCurrentSimulationTime calls
     *
     * @param newTime the new time to return to any getCurrentSimulationTime calls until
     *                reset again
     */
    public void setCurrentSimulationTime( Time newTime ) {
        this.mockSimTime = newTime;
    }

    /**
     * returns a the previously specified fixed time point
     *
     * @return a mock simulation time set by the test drivers for states to observe
     */
    @Override
    public Time getCurrentSimulationTime() {
        return this.mockSimTime;
    }

    /**
     * the time to return to requests for getCurrentSimulationTime() by code under test
     *
     * set by call to setCurrentSimulationTime() by test drivers
     */
    private Time mockSimTime;

    // override all other methods with exceptions to detect units tests that go off the rails
    @Override public void simulate() {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override public void dispatchContext(ActivityJob<?> activityJob) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void dispatchStates(ActivityJob<?> activityJob) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void executeActivity(ActivityJob<?> activityJob) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void addParentChildRelationship(Activity<?> parent, Activity<?> child) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void addActivityListener(Activity<?> target, Activity<?> listener) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void removeActivityListener(Activity<?> target, Activity<?> listener) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void registerStates(List<State<?>> stateList) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void insertIntoQueue(ActivityJob<?> activityJob) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void registerActivityAndJob(Activity<?> activity, ActivityJob<?> activityJob) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public ActivityJob<?> getActivityJob(Activity<?> activity) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public Set<Activity<?>> getActivityListeners(Activity<?> target) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public List<Activity<?>> getActivityChildren(Activity<?> activity) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public void logActivityDuration(Activity<?> activity, Duration d) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" ); }
    @Override
    public Duration getActivityDuration(Activity<?> activity) {
        throw new UnsupportedOperationException( "mock sim engine lacks functionality" );
    }

}
