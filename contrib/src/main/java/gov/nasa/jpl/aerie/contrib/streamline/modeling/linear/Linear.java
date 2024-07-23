package gov.nasa.jpl.aerie.contrib.streamline.modeling.linear;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DummyValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.RecordValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Objects;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;

// TODO: Implement better support for going to/from Linear
public record Linear(Double extract, Double rate) implements Dynamics<Double, Linear> {
  public static ValueMapper<Linear> VALUE_MAPPER = new RecordValueMapper<>(Linear.class, List.of(
      new RecordValueMapper.Component<>("extract", Linear::extract, new DoubleValueMapper()),
      new RecordValueMapper.Component<>("rate", Linear::rate, new DoubleValueMapper())
  ));

  @Override
  public Linear step(Duration t) {
    return linear(extract() + t.ratioOver(SECOND) * rate(), rate());
  }

  public static Linear linear(double value, double rate) {
    return new Linear(value, rate);
  }
}
