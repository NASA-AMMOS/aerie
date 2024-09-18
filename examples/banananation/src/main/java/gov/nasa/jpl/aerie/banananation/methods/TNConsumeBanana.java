package gov.nasa.jpl.aerie.banananation.methods;

import gov.nasa.jpl.aerie.banananation.activities.CompoundConsumeBanana;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Method;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.ActivityReference;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplate;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetworkTemporalConstraint;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.banananation.generated.ActivityReferences;
import java.util.Arrays;
import java.util.HashSet;

@Method
public class TNConsumeBanana implements TaskNetTemplate<CompoundConsumeBanana> {
  @Override
  public TaskNetTemplateData generateTemplate(final CompoundConsumeBanana compoundBanana){
    final ActivityReference pickAct = ActivityReferences.PickBanana(compoundBanana.howMany);
    final ActivityReference biteAct = ActivityReferences.BiteBanana(compoundBanana.biteSize);
    HashSet<ActivityReference> subtasks = new HashSet<>(Arrays.asList(pickAct, biteAct));
    HashSet<TaskNetworkTemporalConstraint> constraints =
        new HashSet<>(Arrays.asList(new TaskNetworkTemporalConstraint.Meets(pickAct, biteAct)));
    return new TaskNetTemplateData(subtasks, constraints);
  }
}
