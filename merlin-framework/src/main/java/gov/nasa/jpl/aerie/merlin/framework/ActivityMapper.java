package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

public interface ActivityMapper<Model, Specification, Return> extends TaskSpecType<Model, Specification, Return> {
  String getName();
}
