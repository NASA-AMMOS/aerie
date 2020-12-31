@Adaptation(module = FooResources.class)

@WithMappers(BasicValueMappers.class)

@WithActivityType(FooActivity.class)

package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BasicValueMappers;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Adaptation.WithActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.annotations.Adaptation.WithMappers;
