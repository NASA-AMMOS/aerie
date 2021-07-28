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
  public final Register<Integer> lineCount = Register.create(0);

  public Mission(final Registrar registrar) {
    registrar.discrete("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.discrete("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    registrar.discrete("/peel", this.peel, new DoubleValueMapper());
    registrar.discrete("/fruit", this.fruit, new DoubleValueMapper());
    registrar.discrete("/plant", this.plant, new IntegerValueMapper());
    registrar.discrete("/producer", this.producer, new StringValueMapper());
  }
}
