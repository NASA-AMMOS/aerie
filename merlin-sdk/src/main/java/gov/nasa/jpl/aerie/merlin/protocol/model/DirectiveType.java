package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface DirectiveType<Model, Directive, Return> {
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Directive instantiate(Map<String, SerializedValue> arguments)
  throws InstantiationException;

  Map<String, SerializedValue> getArguments(Directive directive);
  List<ValidationNotice> getValidationFailures(Directive directive);

  Task<Return> createTask(Model model, Directive directive);
  ValueSchema getReturnValueSchema();
  SerializedValue serializeReturnValue(Return returnValue);

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.createTask(model, this.instantiate(arguments));
   * }
   */
  default Task<Return> createTask(final Model model, final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.createTask(model, this.instantiate(arguments));
  }

  final class UnconstructableDirectiveException extends Exception {
    public UnconstructableDirectiveException(final String message) {
      super(message);
    }
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getValidationFailures(this.instantiate(activity));
   * }
   */
  default List<ValidationNotice> validateArguments(final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.getValidationFailures(this.instantiate(arguments));
  }

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.getArguments(this.instantiate(arguments));
   * }
   */
  default Map<String, SerializedValue> getEffectiveArguments(final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.getArguments(this.instantiate(arguments));
  }
}
