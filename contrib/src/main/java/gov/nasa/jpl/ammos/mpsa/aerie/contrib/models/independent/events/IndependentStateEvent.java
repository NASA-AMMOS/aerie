package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.Objects;

// This can be mechanically derived from `EventHandler`.
public abstract class IndependentStateEvent {
    private IndependentStateEvent() {}

    public abstract <Result> Result visit(IndependentStateEventHandler<Result> visitor);

    public static IndependentStateEvent add(final String stateName, final double amount) {
        Objects.requireNonNull(stateName);
        return new IndependentStateEvent() {
            @Override
            public <Result> Result visit(final IndependentStateEventHandler<Result> visitor) {
                return visitor.add(stateName, amount);
            }
        };
    }

    public static IndependentStateEvent set(final String stateName, final SerializedValue value) {
        Objects.requireNonNull(stateName);
        return new IndependentStateEvent() {
            @Override
            public <Result> Result visit(final IndependentStateEventHandler<Result> visitor) {
                return visitor.set(stateName, value);
            }
        };
    }

    @Override
    public final String toString() {
        return this.visit(new IndependentStateEventHandler<>() {
            @Override
            public String add(final String stateName, final double amount) {
                return String.format("add(\"%s\", %s)",
                        stateName.replace("\\", "\\\\").replace("\"", "\\\""),
                        amount);
            }

            @Override
            public String set(final String stateName, final SerializedValue value) {
                return String.format("set(\"%s\", %s)",
                        stateName.replace("\\", "\\\\").replace("\"", "\\\""),
                        value);
            }
        });
    }

}
