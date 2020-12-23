package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

// TODO: Automatically generate at compile time.
public abstract class Task<$Schema> extends Module<$Schema> {
  private final DynamicCell<Context<$Schema>> context;

  private Task(final DynamicCell<Context<$Schema>> context) {
    super(context);
    this.context = context;
  }

  protected Task() {
    this(DynamicCell.create());
  }

  protected abstract void run(BanananationResources<$Schema> resources);

  public final void runWith(final Context<$Schema> context, final BanananationResources<$Schema> resources) {
    this.context.setWithin(context, () -> this.run(resources));
  }
}
