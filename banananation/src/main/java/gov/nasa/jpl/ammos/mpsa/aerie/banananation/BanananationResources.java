package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.ModuleX;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.RegisterModule;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;

public class BanananationResources<$Schema> extends ModuleX<$Schema> {
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
