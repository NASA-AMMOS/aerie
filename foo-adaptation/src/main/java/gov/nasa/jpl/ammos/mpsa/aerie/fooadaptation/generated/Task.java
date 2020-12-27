package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

// TODO: Automatically generate at compile time.
public abstract class Task<$Schema> extends ModuleX<$Schema> {
  private final DynamicCell<Context<$Schema>> context;

  private Task(final DynamicCell<Context<$Schema>> context) {
    super(context);
    this.context = context;
  }

  protected Task() {
    this(DynamicCell.create());
  }

  protected abstract void run(FooResources<$Schema> resources);

  public final void runWith(final Context<$Schema> context, final FooResources<$Schema> resources) {
    this.context.setWithin(context, () -> this.run(resources));
  }
}
