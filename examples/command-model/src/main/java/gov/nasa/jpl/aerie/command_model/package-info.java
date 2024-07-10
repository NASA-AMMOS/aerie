@MissionModel(model = Mission.class)

@WithConfiguration(Configuration.class)

@WithMappers(BasicValueMappers.class)
@WithMappers(FakeValueMappers.class)

@WithActivityType(AuthoredSequence.class)
@WithActivityType(CommandSpan.class)

package gov.nasa.jpl.aerie.command_model;

import gov.nasa.jpl.aerie.command_model.activities.AuthoredSequence;
import gov.nasa.jpl.aerie.command_model.activities.CommandSpan;
import gov.nasa.jpl.aerie.command_model.value_mappers.FakeValueMappers;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
