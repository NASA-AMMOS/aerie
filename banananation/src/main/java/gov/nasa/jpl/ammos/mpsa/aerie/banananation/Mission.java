package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Register;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;

public class Mission<$Schema> extends Model<$Schema> {
  public final AdditiveRegister<$Schema> fruit;
  public final AdditiveRegister<$Schema> peel;
  public final Register<$Schema, Flag> flag;

  public Mission(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);

    this.flag = Register.create(builder.descend("flag"), Flag.A);
    this.peel = AdditiveRegister.create(builder.descend("peel"), 4.0);
    this.fruit = AdditiveRegister.create(builder.descend("fruit"), 4.0);
  }
}
