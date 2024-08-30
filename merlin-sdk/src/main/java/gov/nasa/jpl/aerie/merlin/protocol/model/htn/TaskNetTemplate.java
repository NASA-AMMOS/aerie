package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

public interface
TaskNetTemplate<T>{
    TaskNetTemplateData generateTemplate(T compound);
  }
