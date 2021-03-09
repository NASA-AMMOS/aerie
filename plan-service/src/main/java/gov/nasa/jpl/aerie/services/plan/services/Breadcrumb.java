package gov.nasa.jpl.aerie.services.plan.services;

import java.util.Objects;

public abstract class Breadcrumb {
  private Breadcrumb() {}

  public interface Visitor<T> {
    T onListIndex(int index);
    T onMapIndex(String index);
  }

  public abstract <T> T match(final Visitor<T> visitor);

  static public Breadcrumb of(final String index) {
    final class MapBreadcrumb extends Breadcrumb {
      private String getIndex() {
        return index;
      }

      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onMapIndex(index);
      }

      @Override
      public boolean equals(final Object object) {
        return (object instanceof MapBreadcrumb && Objects.equals(((MapBreadcrumb)object).getIndex(), index));
      }

      @Override
      public String toString() {
        return "\"" + index + "\"";
      }
    }

    return new MapBreadcrumb();
  }

  static public Breadcrumb of(final int index) {
    final class ListBreadcrumb extends Breadcrumb {
      private int getIndex() {
        return index;
      }

      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onListIndex(index);
      }

      @Override
      public boolean equals(final Object object) {
        return (object instanceof ListBreadcrumb && Objects.equals(((ListBreadcrumb)object).getIndex(), index));
      }

      @Override
      public String toString() {
        return String.valueOf(index);
      }
    }

    return new ListBreadcrumb();
  }
}
