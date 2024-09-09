package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.contrib.models.Accumulator;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.contrib.models.counters.Counter;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.spice.SpiceLoader;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.discreteResource;
import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.realResource;

public final class Mission {
  public final Accumulator fruit;
  public final AdditiveRegister peel;
  public final Register<Flag> flag;
  public final Register<Integer> lineCount = Register.forImmutable(0);
  public final Counter<Integer> plant;
  public final Register<String> producer;
  public final Register<Integer> dataLineCount;

  public Mission(final Registrar registrar, final Configuration config) {
    this.fruit = new Accumulator(config.initialConditions().fruit(), 0.0);
    this.peel = AdditiveRegister.create(config.initialConditions().peel());
    this.flag = Register.forImmutable(config.initialConditions().flag(), new EnumValueMapper<>(Flag.class));
    this.plant = Counter.ofInteger(config.initialPlantCount());
    this.producer = Register.forImmutable(config.initialProducer());
    this.dataLineCount = Register.forImmutable(countLines(config.initialDataPath()));

    registrar.discrete("/flag", this.flag, new EnumValueMapper<>(Flag.class));
    registrar.discrete("/flag/conflicted", this.flag::isConflicted, new BooleanValueMapper());
    discreteResource(registrar, "/peel", this.peel, new DoubleValueMapper(), "kg");
    realResource(registrar, "/fruit", this.fruit, "bananas");
    discreteResource(registrar, "/plant", this.plant, new IntegerValueMapper(), "count");
    registrar.discrete("/producer", this.producer, new StringValueMapper());
    registrar.discrete("/data/line_count", this.dataLineCount, new IntegerValueMapper());
    registrar.topic("/producer", this.producer.ref, new StringValueMapper());

    // Load SPICE in the Mission constructor
    try {
      SpiceLoader.loadSpice();
      System.out.println(CSPICE.ktotal("ALL"));
    } catch (final SpiceErrorException ex) {
      throw new Error(ex);
    }
  }

  private static int countLines(final Path path) {
    try {
      return (int)Files.lines(path).count();
    } catch (IOException e) {
      throw new Error(e);
    }
  }
}
