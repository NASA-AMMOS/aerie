package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Mission {
  public final AdditiveRegister fruit = AdditiveRegister.create(4.0);
  public final AdditiveRegister peel = AdditiveRegister.create(4.0);
  public final Register<Flag> flag = Register.forImmutable(Flag.A);
  public final Register<Integer> lineCount = Register.forImmutable(0);
  public final Counter<Integer> plant;
  public final Register<String> producer;
  public final Register<Integer> dataLineCount;

  public Mission(final Registrar registrar, final Configuration config) {
    this.plant = Counter.ofInteger(config.initialPlantCount());
    this.producer = Register.forImmutable(config.initialProducer());
    this.dataLineCount = Register.forImmutable(countLines(config.initialDataPath()));

    registrar.discrete("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.discrete("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    registrar.discrete("/peel", this.peel, new DoubleValueMapper());
    registrar.discrete("/fruit", this.fruit, new DoubleValueMapper());
    registrar.discrete("/plant", this.plant, new IntegerValueMapper());
    registrar.discrete("/producer", this.producer, new StringValueMapper());
    registrar.discrete("/data/line_count", this.dataLineCount, new IntegerValueMapper());
    registrar.topic("/producer", this.producer.ref, new StringValueMapper());
  }

  private static int countLines(final Path path) {
    try {
      return (int)Files.lines(path).count();
    } catch (IOException e) {
      throw new Error(e);
    }
  }
}
