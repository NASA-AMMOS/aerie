package gov.nasa.jpl.aerie.services.plan.services;

import java.util.ArrayList;
import java.util.List;

public final class BreadcrumbCursor {
  private final List<Breadcrumb> breadcrumbs = new ArrayList<>();

  public List<Breadcrumb> getPath() {
    return new ArrayList<>(this.breadcrumbs);
  }

  public BreadcrumbCursor descend(final int index) {
    this.breadcrumbs.add(Breadcrumb.of(index));
    return this;
  }

  public BreadcrumbCursor descend(final String index) {
    this.breadcrumbs.add(Breadcrumb.of(index));
    return this;
  }

  public BreadcrumbCursor ascend() {
    this.breadcrumbs.remove(this.breadcrumbs.size() - 1);
    return this;
  }
}
