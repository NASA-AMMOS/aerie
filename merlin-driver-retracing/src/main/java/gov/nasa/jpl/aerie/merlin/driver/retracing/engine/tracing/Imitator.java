package gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing;

import gov.nasa.jpl.aerie.merlin.driver.retracing.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.retracing.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;

import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing.TracedTaskFactory.trace;

public class Imitator {
  private final MissionModel<?> missionModel;
  private final Map<SerializedActivity, TaskFactory<?>> taskFactoryCache = new HashMap<>();

  public Imitator(MissionModel<?> missionModel) {
    this.missionModel = missionModel;
  }

  public TaskFactory<?> create(final SerializedActivity serializedDirective) throws InstantiationException {
    if (taskFactoryCache.containsKey(serializedDirective)) {
      return taskFactoryCache.get(serializedDirective);
    } else {
      final TaskFactory<?> taskFactory = trace(missionModel.getTaskFactory(serializedDirective));
      taskFactoryCache.put(serializedDirective, taskFactory);
      return taskFactory;
    }
  }
}
