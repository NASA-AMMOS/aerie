@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)

@WithMappers(BasicValueMappers.class)
@WithMappers(FooValueMappers.class)

@WithActivityType(FooActivity.class)
@WithActivityType(BarActivity.class)
@WithActivityType(DecompositionTestActivities.ParentActivity.class)
@WithActivityType(DecompositionTestActivities.ChildActivity.class)

package gov.nasa.jpl.aerie.fooadaptation;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.fooadaptation.activities.BarActivity;
import gov.nasa.jpl.aerie.fooadaptation.activities.DecompositionTestActivities;
import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.fooadaptation.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
