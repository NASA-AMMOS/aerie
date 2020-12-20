package gov.nasa.jpl.ammos.mpsa.aerie.banananation2;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation2.generated.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states.RegisterModule;

public class BanananationResources<$Schema> extends Module<$Schema>  {
  public final CumulableModule<$Schema> fruit;
  public final CumulableModule<$Schema> peel;
  public final RegisterModule<$Schema, Flag> flag;

  public BanananationResources(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);

    this.flag = RegisterModule.create(builder.descend("flag"), Flag.A);
    this.peel = CumulableModule.create(builder.descend("peel"), 4.0);
    this.fruit = CumulableModule.create(builder.descend("fruit"), 4.0);
  }
}
