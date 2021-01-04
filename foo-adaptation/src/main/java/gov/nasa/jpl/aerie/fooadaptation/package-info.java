@Adaptation(model = Mission.class)

@WithMappers(BasicValueMappers.class)

@WithActivityType(FooActivity.class)

package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithMappers;
