package gov.nasa.jpl.aerie.merlin.server.services;

public interface Breadcrumb {

  interface Visitor<T> {
    T onListIndex(int index);
    T onMapIndex(String index);
  }

  <T> T match(final Visitor<T> visitor);

  static Breadcrumb of(final String index) {
    record MapBreadcrumb(String index) implements Breadcrumb {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onMapIndex(index);
      }
    }

    return new MapBreadcrumb(index);
  }

  static Breadcrumb of(final int index) {
    record ListBreadcrumb(int index) implements Breadcrumb {
      @Override
      public <T> T match(final Visitor<T> visitor) {
        return visitor.onListIndex(index);
      }
    }

    return new ListBreadcrumb(index);
  }
}
