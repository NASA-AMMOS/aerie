@Adaptation(model = Mission.class)

@WithMappers(BasicValueMappers.class)

@WithActivityType(BiteBananaActivity.class)
@WithActivityType(PeelBananaActivity.class)
@WithActivityType(ParameterTestActivity.class)
@WithActivityType(PickBananaActivity.class)
@WithActivityType(ChangeProducerActivity.class)

package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.PickBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.ChangeProducerActivity;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithMappers;
