package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.power.RandomAccessState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Map;

/**
 * simple mock for state class that just returns the latest specified value
 *
 * where "latest" means last in execution order, not last in simulation time!
 *
 * @param <T> the datatype of the value to return on any query
 */
public class MockState<T> implements RandomAccessState<T> {

    /**
     * creates a new MockState that just returns the provided value
     *
     * @param value the value to return to get() calls
     */
    public MockState( T value ) {
        this.value = value;
    }

    /**
     * resets the value that will be returned to subsequent get() calls
     *
     * @param value the new value to return on subsuequent get() calls
     */
    public void setMockValue(T value) {
        this.value = value;
    }

    /**
     * the stored value that is returned to any subsequent get() calls
     */
    private T value;

    /**
     * just returns the previously stored value
     *
     * @return the previously stored value
     */
    @Override
    public T get() {
        return value;
    }

    /**
     * just returns the previously stored value
     *
     * @param queryTime time time of the query
     * @return the previously stored value
     */
    @Override
    public T get( Instant queryTime ) {
        return value;
    }


    /**
     * required by engine but not used by the mock
     *
     * @param engine the controlling simulation engine
     */
    @Override
    public void setEngine(SimulationEngine engine) { }

    /**
     * required by engine but not used by the mock
     *
     * @return null
     */
    @Override
    public Map<Instant, T> getHistory() { return null; }

    /**
     * required by api but not used by the mock
     *
     * @return the java object id
     */
    @Override
    public String getName() {
        return super.toString();
    }

}
