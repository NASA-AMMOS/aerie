package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events;

public interface DefaultIndependentStateEventHandler<Result> extends IndependentStateEventHandler<Result> {
    Result unhandled();

    @Override
    default Result add(final String binName, final double amount) {
        return this.unhandled();
    }

    @Override
    default Result set(final String binName, final double value) {
        return this.unhandled();
    }
}
