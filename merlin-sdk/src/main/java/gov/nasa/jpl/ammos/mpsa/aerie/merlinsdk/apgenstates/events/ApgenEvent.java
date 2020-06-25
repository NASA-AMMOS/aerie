package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events;

import java.util.Objects;

// This can be mechanically derived from `EventHandler`.
public abstract class ApgenEvent {
    private ApgenEvent() {}

    public abstract <Result> Result visit(ApgenEventHandler<Result> visitor);

    public static ApgenEvent add(final String stateName, final double amount) {
        Objects.requireNonNull(stateName);
        return new ApgenEvent() {
            @Override
            public <Result> Result visit(final ApgenEventHandler<Result> visitor) {
                return visitor.add(stateName, amount);
            }
        };
    }

    public static ApgenEvent set(final String stateName, final double value) {
        Objects.requireNonNull(stateName);
        return new ApgenEvent() {
            @Override
            public <Result> Result visit(final ApgenEventHandler<Result> visitor) {
                return visitor.set(stateName, value);
            }
        };
    }

    @Override
    public final String toString() {
        return this.visit(new ApgenEventHandler<>() {
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
