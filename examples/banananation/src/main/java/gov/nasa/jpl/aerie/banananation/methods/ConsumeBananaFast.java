package gov.nasa.jpl.aerie.banananation.methods;

import gov.nasa.jpl.aerie.banananation.activities.CompoundConsumeBanana;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ParametricDecomposition;
import gov.nasa.jpl.aerie.merlin.framework.htn.ActivityInstantiation;
import gov.nasa.jpl.aerie.merlin.framework.htn.ActivityReference;
import gov.nasa.jpl.aerie.merlin.framework.htn.Method;
import gov.nasa.jpl.aerie.merlin.framework.htn.TaskNetworkConstraint;

import java.util.Map;
import java.util.Set;

import static gov.nasa.jpl.aerie.merlin.framework.htn.ActivityInstantiation.of;
import static gov.nasa.jpl.aerie.merlin.framework.htn.TaskNetworkConstraint.precedes;

public class ConsumeBananaFast implements Method<CompoundConsumeBanana> {
  final ActivityReference pickAct = new ActivityReference("pick", "PickBanana");
  final ActivityReference biteAct = new ActivityReference("bite", "BiteBanana");

  //TODO: probably a bunch of validation work on the parameter maps
  //TODO: not very beautiful this mapping from the string name of the parameter to a instantiated parameter...
  //REVIEW: there is no choice over a current state or something, it's purely dependent on the parameters of the compounds
  //if we add arbitrary code that can access the Mission, then we need to simulate to decompose...
  //For me, the whole logic should be declarative and put into constraints
  @Override
  @ParametricDecomposition
  public Set<ActivityInstantiation> getSubtasks(final CompoundConsumeBanana compoundConsumeBanana) {
    return Set.of(
        of(
            pickAct,
            //routing from compound parameter to activity parameter
            //can we actually parse that ? or just use this method to instantiate ?
            Map.of("quantity", compoundConsumeBanana.howMany)
        ),
        of(
            biteAct,
            Map.of("biteSize", compoundConsumeBanana.howMany)
        )
    );
  }

  @Override
  public Set<TaskNetworkConstraint> getConstraints(final CompoundConsumeBanana compoundConsumeBanana) {
    return Set.of(
        precedes(pickAct, biteAct)
    );
  }
}
