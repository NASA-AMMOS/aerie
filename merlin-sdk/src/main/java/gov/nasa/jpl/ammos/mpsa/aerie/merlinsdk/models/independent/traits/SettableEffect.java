package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.traits;

import java.util.Objects;

public abstract class SettableEffect<T> {
    private SettableEffect() {}

    public abstract <Result> Result visit(Visitor<T, Result> visitor);

    public interface Visitor<T, Result> {
        Result empty();
        Result setTo(T base);
        Result conflict();
    }

    public static <T> SettableEffect<T> empty() {
        return new SettableEffect<>() {
            @Override
            public <Result> Result visit(final Visitor<T, Result> visitor) {
                return visitor.empty();
            }
        };
    }

    public static <T> SettableEffect<T> setTo(final T base) {
        Objects.requireNonNull(base);

        return new SettableEffect<>() {
            @Override
            public <Result> Result visit(final Visitor<T, Result> visitor) {
                return visitor.setTo(base);
            }
        };
    }

    public static <T> SettableEffect<T> conflict() {
        return new SettableEffect<>() {
            @Override
            public <Result> Result visit(final Visitor<T, Result> visitor) {
                return visitor.conflict();
            }
        };
    }

    public final boolean isEmpty() {
        return this.visit(new Visitor<>() {
            @Override
            public Boolean empty() {
                return true;
            }

            @Override
            public Boolean setTo(final T base) {
                return false;
            }

            @Override
            public Boolean conflict() {
                return false;
            }
        });
    }

    @Override
    public final String toString() {
        return this.visit(new Visitor<>() {
            @Override
            public String empty() {
                return "empty()";
            }

            @Override
            public String setTo(final T base) {
                return "setTo(" + base + ")";
            }

            @Override
            public String conflict() {
                return "conflict()";
            }
        });
    }
}
