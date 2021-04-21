package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

public final class Mission {
  public final AdditiveRegister fruit = AdditiveRegister.create(4.0);
  public final AdditiveRegister peel = AdditiveRegister.create(4.0);
  public final Register<Flag> flag = Register.create(Flag.A);
  public final Counter<Integer> plant = Counter.ofInteger(200);
  public final Register<String> producer = Register.create("Chiquita");

  public Mission(final Registrar registrar) {
    registrar.resource("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.resource("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    registrar.resource("/peel", this.peel, new DoubleValueMapper());
    registrar.resource("/fruit", this.fruit, new DoubleValueMapper());
    registrar.resource("/plant", this.plant, new IntegerValueMapper());
    registrar.resource("/producer", this.producer, new StringValueMapper());
  }
}
