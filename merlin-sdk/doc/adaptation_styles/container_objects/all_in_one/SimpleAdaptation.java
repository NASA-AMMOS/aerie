
import Constraints.*;
import StateDefinitions.*;
import ActivityDefinitionMetadata.*;

enum TimeUnits{
	HOURS, MINUTES;
}

enum Modes{
	STANDBY, ACHIEVED_MODE;
}

enum Rates{
	BITS_PER_SECOND;



public static void main(String[] args){


	MissionAdaptaton clipper = new MissionAdaptaton();
	clipper.setName("Europa Clipper Mission");

	ActivityDefinition UVS_CAL_BSR = new ActivityDefinition();
	UVS_CAL_BSR.setName("UVS Bright Star Raster Calibration")
	
	//UVS_METADATA comes from ActvityDDefinitionMetadata

	UVS_CAL_BSR.setMetadata(UVS_METADATA)
	UVS_CAL_BSR.setReference("https://github.jpl.nasa.gov/Europa/OPS/blob/0bc8a70f29d4c74e27b714f1c67c359ce8d9ea14/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L545", 
		"https://charlie-lib.jpl.nasa.gov/docushare/dsweb/Get/Document-2165712/UVS%20Instrument%20Ops%20Summary%202017-01-13.docx");
	
	//What if we allowed activities to have an "estimated duration"?  
	//An activity may take longer than what the adapter estimates, if the adapter wants 
	//to have the duration be strictly enforced they can have it as a constraint
	//we can create durative constraints on activities that way
	//perhaps we can have different setters for the activity def class, setFixed will create a constraint
	//this constraint will either be violated or not violated 
	UVS_CAL_BSR.setFixedDuration(6.0);
	UVS_CAL_BSR.setUnits(TimeUnits.HOURS);

	//vs.

	//this is the adapter's estimated duration
	//it is more for them to gauge how their estimate measures up with the actual modeled
	//it is likely in most scenarios there is no difference
	//however, a report generating differences in estimated vs actual may be helpful at the plan level
	UVS_CAL_BSR.setEstimatedDuration(6.0);
	UVS_CAL_BSR.setUnits(TimeUnits.HOURS);

	//vs.

	//this is for a smarter scheduler
	//I'm not sure what the difference beetween utility and priority is in Cara's ADSchema
	UVS_CAL_BSR.setOptimalDuration(6.0);
	UVS_CAL_BSR.setPriority(5);
	UVS_CAL_BSR.setUnits(TimeUnits.HOURS);

	//this would be defined in the constraint file 
	//for common constraints types (e.g. num activities in a phase, an activity preceding another activity, etc),
	//we can create different subclasses
	//an argument be that it might not be intuitive for the user to become instantly familiar with all these subclasses
	//so we may want to minimize the number of constraints we have, and allow the subclasses to accomodate several types of constraints if possible
	//e.g. ONE temporal constraint class vs a preceding activity constraint class and a following class constraint
	GlobalConstraint PER_MISSION_COUNT = new CountOverPhase();
	PER_MISSION_COUNT.setName("Number calibrations per missions")
	PER_MISSION_COUNT.setType(Integer.class)
	
	PER_MISSION_COUNT.setFixedValue(1)

	//vs.

	//dummy data, not reflective of Cara's code
	PER_MISSION_COUNT.setMaxValue(3)
	PER_MISSION_COUNT.setMinCalue(0)



	// UVS_CALIBRATION_CONSTRAINTS is defined elsewhere
	UVS_CAL_BSR.addConstraints(PER_MISSION_COUNT, UVS_CALIBRATION_CONSTRAINTS)

	//also allowed (constructor uses varargs)
	UVS_CAL_BSR.addConstraints(PER_MISSION_COUNT);
	UVS_CAL_BSR.addConstraints(UVS_CALIBRATION_CONSTRAINTS);




	//we can eiether 
	EffectModelDefinition UVS_CAL_BSR_EFFECTS = new EffectModelDefinition();
	UVS_CAL_BSR_EFFECTS.setName("Sends effect requests for UVS CAL BSR activity")

	StateDefinition OP_MODE = new StateDefinition();
	OP_MODE.setName("operational mode");
	OP_MODE.setType(Enum.class)
	OP_MODE.setDefaultValue(TimeUnits.STANDBY);
	//Q: two of the same opModes

	StateDefinition ACHIEVED_MODE = new StateDefinition();
	ACHIEVED_MODE.setName("acheived mode");
	ACHIEVED_MODE.setType(Enum.class<Modes>)
	ACHIEVED_MODE.setDefaultValue(Modes.ACHIEVED_MODE)

	StateDefinition DATA_PRODUCTION_RATE = new StateDefinition();
	DATA_PRODUCTION_RATE.setName("effective data proudction rate")
	DATA_PRODUCTION_RATE.setType(Double.class)
	DATA_PRODUCTION_RATE.setUnits(Rates.BITS_PER_SECOND)
	//code should be able to type convert below string to double
	DATA_PRODUCTION_RATE.setDefaultValue("92.6E3")
	
	//lambda method validator: 
	//this validator will be run everytime this state value is changed
	//if the validator fails after any chance, a report should capture that
	//we should probably have different violation types in our report (constraint, state value, etc)
	DATA_PRODUCTION_RATE.setValidatin(v->(v<30.0))
	//will also allow users to pass in methods/Java8 validation

	StateDefinitionGroup UVS_STATE_DEFS = new StateDefinitionGroup(OP_MODE, ACHIEVED_MODE, DATA_PRODUCTION_RATE);

	UVS_CAL_BSR.addStateDefinitons(UVS_STATE_DEFS);
	//we can also discuss not attaching state definitions to the activity and just the effect model
	UVS_CAL_BSR_EFFECTS.addStateDefinitons(UVS_STATE_DEFS);

	ParameterDefinition rightAscension_rad = new ParameterDefinition();
	rightAscension_rad.setName("right ascension angle in radians");
	rightAscension_rad.setDescription("the angle measured in radians eastward from the sun at the "
      +" specified epoch vernal equinox along the celestial equator to the "
      +" hour cicle that passes through the target point; essentially "
      +" celestial longitude");
	rightAscension_rad.setType(float.class);

	//dummy values
	//from Steve's CelestialCoordinates
	rightAscension_rad.setDefaultValue(ra_rad);
	//if any of these get violated there shuld be a report 
	rightAscension_rad.setValidatin(v->(v<30.0))
	rightAscension_rad.setMax(22.2);

	ParameterDefinition referenceFrame = new ParameterDefinition();
	referenceFrame.setName("astronomical reference frame");
	referenceFrame.setDefaultValue(epoch);
	referenceFrame.setDescription("the frame of reference (and epoch, if needed) used to measure the "
      +"target pointing coordinates, for example J2000.0 or ICRS");

	ParameterDefinitonGroup CELESTIAL_COORDINATES = new ParameterDefinitonGroup(rightAscension_rad, referenceFrame);

	UVS_CAL_BSR.setParameters(CELESTIAL_COORDINATES);
	//I think we do have to add these parameters to the ActivityDef because their value will change depending
	//on the activity def "instance"
	UVS_CAL_BSR_EFFECTS.setParameters(CELESTIAL_COORDINATES);

	//still need to add state controller
	//still need to showe decomp
	//still need command expansion example
	//still need to specify scheduling logic
	//also need to decide how to break up the code in this over various files
	//seems like I prefer grouping by object type, Basak prefers by Activity type 
	//(I think obj type is more reusable but perhaps there way we can show users all the avialable, reusable objects)
	//our backend should also make sure user isn't creating duplicate objects (e.g. two objects attached to diff activity types with same vparams)












}

