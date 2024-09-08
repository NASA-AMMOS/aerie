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
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.discreteResource;
import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.realResource;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

public final class Mission {
  public final Accumulator fruit;
  public final AdditiveRegister peel;
  public final Register<Flag> flag;
  public final Register<Integer> lineCount = Register.forImmutable(0);
  public final Counter<Integer> plant;
  public final Register<String> producer;
  public final Register<Integer> dataLineCount;
  public final Optional<Register<Integer>> numCell;
  public final Optional<Register<String>> stringCell;

  public Mission(final Registrar registrar, final Configuration config) {

    if (config.includeNumCell()) {
      this.numCell = Optional.of(Register.forImmutable(0));
      registrar.topic("numCell", this.numCell.get().ref, new IntegerValueMapper());
    } else {
      this.numCell = Optional.empty();
    }

    if (config.includeStringCell()) {
      this.stringCell = Optional.of(Register.forImmutable(""));
      registrar.topic("stringCell", this.stringCell.get().ref, new StringValueMapper());
    } else {
      this.stringCell = Optional.empty();
    }

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

    spawn(() -> {
      var stepCount = 0;
      while (true) {
        final var currentPlant = this.plant.get();
        waitUntil(this.plant.is(currentPlant).not());
        this.peel.add(stepCount - this.peel.get());
        stepCount += 1;
      }
    });

    spawn(() -> {
      while (true) {
        final var currentPlant = this.plant.get();
        waitUntil(this.plant.is(currentPlant).not());
        if (config.includeNumCell()) {
          final var numCell = this.numCell.get();
          numCell.set(numCell.get() + 1);
        }
        if (config.includeStringCell()) {
          final var stringCell = this.stringCell.get();
          stringCell.set(stringCell.get());
        }
      }
    });
  }

  private static int countLines(final Path path) {
    try {
      return (int)Files.lines(path).count();
    } catch (IOException e) {
      throw new Error(e);
    }
  }
}
