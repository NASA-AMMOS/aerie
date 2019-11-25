package gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils;

import java.util.Objects;

public abstract class Breadcrumb {
  private Breadcrumb() {}

  public interface Visitor<Output, Throws extends Throwable> {
    Output onListIndex(int index) throws Throws;
    Output onMapIndex(String index) throws Throws;
  }

  /**
   * A "safe" Visitor is one which throws no checked exceptions. This interface is purely
   * an aide to type inference, allowing {@code SafeVisitor<>} to be written instead of the much
   * more verbose {@code Visitor<ResultType, Error>}.
   */
  public interface SafeVisitor<Output> extends Visitor<Output, Error> {
  }

  public abstract <Output, Throws extends Throwable>
  Output match(final Visitor<Output, Throws> visitor) throws Throws;

  static public Breadcrumb of(final String index) {
    Objects.requireNonNull(index);
    return new Breadcrumb() {
      @Override
      public <Output, Throws extends Throwable>
      Output match(final Visitor<Output, Throws> visitor) throws Throws {
        return visitor.onMapIndex(index);
      }
    };
  }

  static public Breadcrumb of(final int index) {
    return new Breadcrumb() {
      @Override
      public <Output, Throws extends Throwable>
      Output match(final Visitor<Output, Throws> visitor) throws Throws {
        return visitor.onListIndex(index);
      }
    };
  }


  public enum Kind {
    ListIndex, MapIndex;

    @Override
    public String toString() {
      switch (this) {
        case ListIndex: return "ListIndex";
        case MapIndex: return "MapIndex";
        default: throw new Error("Unexpected enum value of type " + this.getClass().getSimpleName());
      }
    }
  }

  public Kind getKind() {
    return this.match(new SafeVisitor<>() {
      @Override
      public Kind onListIndex(final int index) {
        return Kind.ListIndex;
      }

      @Override
      public Kind onMapIndex(final String index) {
        return Kind.MapIndex;
      }
    });
  }

  // SAFETY: If equals is overridden, then hashCode must also be overridden.
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Breadcrumb)) return false;
    final Breadcrumb other = (Breadcrumb)o;

    return this.match(new SafeVisitor<>() {
      @Override
      public Boolean onListIndex(final int index1) {
        return other.match(new SafeVisitor<>() {
          @Override
          public Boolean onListIndex(final int index2) {
            return (index1 == index2);
          }

          @Override
          public Boolean onMapIndex(final String _index) {
            return false;
          }
        });
      }

      @Override
      public Boolean onMapIndex(final String index1) {
        return other.match(new SafeVisitor<>() {
          @Override
          public Boolean onListIndex(final int _index) {
            return false;
          }

          @Override
          public Boolean onMapIndex(final String index2) {
            return Objects.equals(index1, index2);
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
      public Integer onListIndex(final int index) {
        return Objects.hash(kind, index);
      }

      @Override
      public Integer onMapIndex(final String index) {
        return Objects.hash(kind, index);
      }
    });
  }

  @Override
  public String toString() {
    final var kind = this.getKind();
    return this.match(new SafeVisitor<>() {
      @Override
      public String onListIndex(final int index) {
        return kind.toString() + "(" + index + ")";
      }

      @Override
      public String onMapIndex(final String index) {
        return kind.toString() + "(" + index + ")";
      }
    });
  }
}
