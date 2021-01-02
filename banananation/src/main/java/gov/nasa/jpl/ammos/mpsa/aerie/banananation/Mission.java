package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.Register;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Registrar;

public class Mission<$Schema> extends Model<$Schema> {
  public final AdditiveRegister<$Schema> fruit;
  public final AdditiveRegister<$Schema> peel;
  public final Register<$Schema, Flag> flag;

  public Mission(final Registrar<$Schema> registrar) {
    super(registrar);

    this.flag = Register.create(registrar.descend("flag"), Flag.A);
    this.peel = AdditiveRegister.create(registrar.descend("peel"), 4.0);
    this.fruit = AdditiveRegister.create(registrar.descend("fruit"), 4.0);
  }
}
