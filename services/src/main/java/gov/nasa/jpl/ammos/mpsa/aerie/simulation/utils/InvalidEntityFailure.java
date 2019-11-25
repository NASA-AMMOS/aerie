package gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils;

import javax.json.Json;
import java.util.List;
import java.util.Objects;

public abstract class InvalidEntityFailure {
  private InvalidEntityFailure() {}

  public interface Visitor<Output, Throws extends Throwable> {
    Output onScope(Breadcrumb breadcrumb, List<InvalidEntityFailure> scope) throws Throws;
    Output onMessage(String value) throws Throws;
  }

  /**
   * A "safe" {@link Visitor} is one which throws no checked exceptions. This interface is purely
   * an aide to type inference, allowing {@code SafeVisitor<>} to be written instead of the much
   * more verbose {@code Visitor<ResultType, Error>}.
   */
  public interface SafeVisitor<Output> extends Visitor<Output, Error> {
  }

  public abstract <Output, Throws extends Throwable>
  Output match(final Visitor<Output, Throws> visitor) throws Throws;

  static public InvalidEntityFailure scope(final Breadcrumb breadcrumb, final List<InvalidEntityFailure> scope) {
    Objects.requireNonNull(breadcrumb);
    final var scopeCopy = List.copyOf(scope);

    return new InvalidEntityFailure() {
      @Override
      public <Output, Throws extends Throwable>
      Output match(final Visitor<Output, Throws> visitor) throws Throws {
        return visitor.onScope(breadcrumb, scopeCopy);
      }
    };
  }

  static public InvalidEntityFailure message(final String message) {
    Objects.requireNonNull(message);

    return new InvalidEntityFailure() {
      @Override
      public <Output, Throws extends Throwable>
      Output match(final Visitor<Output, Throws> visitor) throws Throws {
        return visitor.onMessage(message);
      }
    };
  }


  public enum Kind {
    Scope, Message;

    @Override
    public String toString() {
      switch (this) {
        case Scope: return "Scope";
        case Message: return "Message";
        default: throw new Error("Unexpected enum value of type " + this.getClass().getSimpleName());
      }
    }
  }

  public Kind getKind() {
    return this.match(new Visitor<Kind, Error>() {
      @Override
      public Kind onScope(final Breadcrumb _breadcrumb, final List<InvalidEntityFailure> _scope) {
        return Kind.Scope;
      }

      @Override
      public Kind onMessage(final String _value) {
        return Kind.Message;
      }
    });
  }

  // SAFETY: If equals is overridden, then hashCode must also be overridden.
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof InvalidEntityFailure)) return false;
    final InvalidEntityFailure other = (InvalidEntityFailure)o;

    return this.match(new SafeVisitor<>() {
      @Override
      public Boolean onScope(final Breadcrumb breadcrumb1, final List<InvalidEntityFailure> scope1) {
        return other.match(new SafeVisitor<>() {
          @Override
          public Boolean onScope(final Breadcrumb breadcrumb2, final List<InvalidEntityFailure> scope2) {
            return Objects.equals(breadcrumb1, breadcrumb2) && Objects.equals(scope1, scope2);
          }

          @Override
          public Boolean onMessage(final String _value) {
            return false;
          }
        });
      }

      @Override
      public Boolean onMessage(final String value1) {
        return other.match(new SafeVisitor<>() {
          @Override
          public Boolean onScope(final Breadcrumb _breadcrumb, final List<InvalidEntityFailure> _scope) {
            return false;
          }

          @Override
          public Boolean onMessage(final String value2) {
            return Objects.equals(value1, value2);
          }
        });
      }
    });
  }

  @Override
  public int hashCode() {
    final var kind = this.getKind();

    return this.match(new SafeVisitor<>() {
      @Override
      public Integer onScope(final Breadcrumb breadcrumb, final List<InvalidEntityFailure> scope) {
        return Objects.hash(kind, breadcrumb, scope);
      }

      @Override
      public Integer onMessage(final String value) {
        return Objects.hash(kind, value);
      }
    });
  }

  @Override
  public String toString() {
    final var kind = this.getKind();

    return this.match(new SafeVisitor<>() {
      @Override
      public String onScope(final Breadcrumb breadcrumb, final List<InvalidEntityFailure> scope) {
        return kind.toString() + "(" + breadcrumb.toString() + ", " + scope.toString() + ")";
      }

      @Override
      public String onMessage(final String value) {
        // Pass through JSON to produce an escaped string
        return kind.toString() + "(" + Json.createValue(value).toString() + ")";
      }
    });
  }
}
