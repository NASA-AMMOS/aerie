package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface DirectiveType<Model, Arguments, Result> {
  InputType<Arguments> getInputType();

  // TODO: Remove this, since anything that currently depends on this
  //   ought to instead depend on the topics exported by a model on which
  //   the output data is emitted.
  OutputType<Result> getOutputType();

  Task<Result> createTask(Model model, Arguments arguments);

  /**
   * This method must behave as though implemented as:
   * {@snippet :
   * return this.createTask(model, this.getInputType().instantiate(arguments));
   * }
   */
  default Task<Result> createTask(final Model model, final Map<String, SerializedValue> arguments)
  throws InstantiationException
  {
    return this.createTask(model, this.getInputType().instantiate(arguments));
  }
}
