package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

public interface DefaultApgenEventHandler<Result> extends ApgenEventHandler<Result> {
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
