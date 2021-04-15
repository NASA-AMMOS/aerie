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
  public final AdditiveRegister fruit;
  public final AdditiveRegister peel;
  public final Register<Flag> flag;
  public final Counter<Integer> plant;
  public final Register<String> producer;

  public Mission(final Registrar registrar) {
    this.flag = Register.create(Flag.A);
    this.peel = AdditiveRegister.create(4.0);
    this.fruit = AdditiveRegister.create(4.0);
    this.plant = Counter.ofInteger(200);
    this.producer = Register.create("Chiquita");

    registrar.resource("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.resource("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    registrar.resource("/peel", this.peel, new DoubleValueMapper());
    registrar.resource("/fruit", this.fruit, new DoubleValueMapper());
    registrar.resource("/plant", this.plant, new IntegerValueMapper());
    registrar.resource("/producer", this.producer, new StringValueMapper());
  }
}
