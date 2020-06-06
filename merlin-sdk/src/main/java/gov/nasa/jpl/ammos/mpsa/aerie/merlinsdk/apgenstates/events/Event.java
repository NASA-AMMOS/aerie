package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

import java.util.Objects;

// This can be mechanically derived from `EventHandler`.
public abstract class Event {
    private Event() {}

    public abstract <Result> Result visit(EventHandler<Result> visitor);

    public static Event add(final String stateName, final double amount) {
        Objects.requireNonNull(stateName);
        return new Event() {
            @Override
            public <Result> Result visit(final EventHandler<Result> visitor) {
                return visitor.add(stateName, amount);
            }
        };
    }

    public static Event set(final String stateName, final double value) {
        Objects.requireNonNull(stateName);
        return new Event() {
            @Override
            public <Result> Result visit(final EventHandler<Result> visitor) {
                return visitor.set(stateName, value);
            }
        };
    }

    @Override
    public final String toString() {
        return this.visit(new EventHandler<>() {
            @Override
            public String add(final String stateName, final double amount) {
                return String.format("add(\"%s\", %s)",
                        stateName.replace("\\", "\\\\").replace("\"", "\\\""),
                        amount);
            }

            @Override
            public String set(final String stateName, final double value) {
                return String.format("set(\"%s\", %s)",
                        stateName.replace("\\", "\\\\").replace("\"", "\\\""),
                        value);
            }
        });
    }

}
