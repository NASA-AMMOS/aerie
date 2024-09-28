package gov.nasa.ammos.aerie.merlin.driver.test;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public class Stubs {
  public static final InputType<Unit> UNIT_INPUT_TYPE = stubInputType(Unit.UNIT);
  public static final OutputType<Unit> UNIT_OUTPUT_TYPE = stubOutputType();

  public static final InputType<Object> OBJECT_INPUT_TYPE = stubInputType(new Object());
  public static final OutputType<Object> OBJECT_OUTPUT_TYPE = stubOutputType();

  public static final InputType<Map<String, SerializedValue>> PASS_THROUGH_INPUT_TYPE =
      new InputType<>() {
        @Override
        public List<Parameter> getParameters() {
          return List.of();
        }

        @Override
        public List<String> getRequiredParameters() {
          return List.of();
        }

        @Override
        public Map<String, SerializedValue> instantiate(final Map<String, SerializedValue> arguments) {
          return arguments;
        }

        @Override
        public Map<String, SerializedValue> getArguments(final Map<String, SerializedValue> value) {
          return Map.of();
        }

        @Override
        public List<ValidationNotice> getValidationFailures(final Map<String, SerializedValue> value) {
          return List.of();
        }
      };

  public static <T> InputType<T> stubInputType(T defaultValue) {
    return new InputType<>() {
      @Override
      public List<Parameter> getParameters() {
        return List.of();
      }

      @Override
      public List<String> getRequiredParameters() {
        return List.of();
      }

      @Override
      public T instantiate(final Map<String, SerializedValue> arguments) {
        return defaultValue;
      }

      @Override
      public Map<String, SerializedValue> getArguments(final T value) {
        return Map.of();
      }

      @Override
      public List<ValidationNotice> getValidationFailures(final T value) {
        return List.of();
      }
    };
  }

  public static <T> OutputType<T> stubOutputType() {
    return new OutputType<>() {
      @Override
      public ValueSchema getSchema() {
        return ValueSchema.ofStruct(Map.of());
      }

      @Override
      public SerializedValue serialize(final T value) {
        return SerializedValue.of(Map.of());
      }
    };
  }
}
