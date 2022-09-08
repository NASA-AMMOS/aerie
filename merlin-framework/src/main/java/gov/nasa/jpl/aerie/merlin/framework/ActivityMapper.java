package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;

public interface ActivityMapper<Model, Specification, Return> extends DirectiveType<RootModel<Model>, Specification, Return> {
  Topic<Specification> getInputTopic();
  Topic<Return> getOutputTopic();

  Context.TaskFactory<Return> getTaskFactory(final Model model, final Specification activity);
}
