package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.Model;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

public class Mission extends Model {
  public final AdditiveRegister fruit;
  public final AdditiveRegister peel;
  public final Register<Flag> flag;

  public Mission(final Registrar registrar) {
    super(registrar);

    this.flag = Register.create(registrar, Flag.A);
    this.peel = AdditiveRegister.create(registrar, 4.0);
    this.fruit = AdditiveRegister.create(registrar, 4.0);

    registrar.resource("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.resource("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    registrar.resource("/peel", this.peel, new DoubleValueMapper());
    registrar.resource("/fruit", this.fruit, new DoubleValueMapper());
  }
}
