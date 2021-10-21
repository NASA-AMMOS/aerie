package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.model.DiscreteApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.RealApproximator;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.*;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.duration;

/**
 * A facade for simulating plans and processing simulation results.
 * Includes : (1) providing resulting resource values to scheduler constructs
 * (2) providing durations of activity instances
 */
public class SimulationFacade {

    /**
     * Accessor for integer resource feeders
     * @param resourceName the name of the resource
     * @return the resource feeder if it exists, null otherwise
     */
    public SimResource<Integer> getIntResource(String resourceName){
        if(!feedersInt.containsKey(resourceName)){
            feedersInt.put(resourceName, new SimResource<Integer>());
        }
        return feedersInt.get(resourceName);
    }

    /**
     * Accessor for double resource feeders
     * @param resourceName the name of the resource
     * @return the resource feeder if it exists, null otherwise
     */
    public SimResource<Double> getDoubleResource(String resourceName){
        if(!feedersDouble.containsKey(resourceName)){
            feedersDouble.put(resourceName, new SimResource<Double>());
        }
        return feedersDouble.get(resourceName);
    }

    /**
     * Accessor for string resource feeders
     * @param resourceName the name of the resource
     * @return the resource feeder if it exists, null otherwise
     */
    public SimResource<String> getStringResource(String resourceName){
        if(!feedersString.containsKey(resourceName)){
            feedersString.put(resourceName, new SimResource<String>());
        }
        return feedersString.get(resourceName);
    }

    /**
     * Accessor for boolean resource feeders
     * @param resourceName the name of the resource
     * @return the resource feeder if it exists, null otherwise
     */
    public SimResource<Boolean> getBooleanResource(String resourceName){
        if(!feedersBool.containsKey(resourceName)){
            feedersBool.put(resourceName, new SimResource<Boolean>());
        }
        return feedersBool.get(resourceName);
    }

    public SimulationFacade(Range<Time> planningHorizon, Adaptation<?, ?> adaptation){
        this.adaptation = adaptation;
        this.planningHorizon = planningHorizon;
        feedersInt = new HashMap<String, SimResource<Integer>>() ;
        feedersDouble = new HashMap<String, SimResource<Double>>() ;
        feedersBool = new HashMap<String, SimResource<Boolean>> ();
        feedersString = new HashMap<String, SimResource<String>> ();
    }


    /**
     * Fetches activity instance durations from last simulation
     * @param activityInstance the activity instance we want the duration for
     * @return the duration if found in the last simulation, null otherwise
     */
    public gov.nasa.jpl.aerie.scheduler.Duration getActivityDuration(ActivityInstance activityInstance){
        if(lastResults == null){
            System.out.println("You need to simulate before requesting activity duration");
        }
        var simAct = lastResults.simulatedActivities.get(activityInstance.getName());
        if(simAct!= null) {
           long durMilli = simAct.duration.in(Duration.MILLISECOND);
           gov.nasa.jpl.aerie.scheduler.Duration schedDur = gov.nasa.jpl.aerie.scheduler.Duration.fromMillis(durMilli);
            return schedDur;
        } else{
            System.out.println("Simulation has been launched but activity with name= " + activityInstance.getName() + " has not been found");
        }
        return null;
    }

    /**
     * Simulates schedule and processes results to produce accessors for resources and activities. The complete set of activities scheduled so
     * far should be passed for every simulation. Simulation runs for the whole planning horizon.
     * @param plan the plan to simulate
     */
    public void simulatePlan(Plan plan){

        final var actsInPlan = plan.getActivitiesByTime();
        final var schedule = new HashMap<String, Pair<Duration, SerializedActivity>>();

        for(var act : actsInPlan){

            Map<String, SerializedValue> params = new HashMap<String, SerializedValue>();
            act.getParameters().forEach( (name,value)->  params.put(name, this.serialize(value)));
            schedule.put(act.getName(),Pair.of(
                    duration(act.getStartTime().minus(this.planningHorizon.getMinimum()).toMicroseconds(), MICROSECONDS),
                    new SerializedActivity(act.getType().getName(),params)));
        }

        final var simulationDuration = duration(planningHorizon.getMaximum().minus(planningHorizon.getMinimum()).toMicroseconds(), MICROSECONDS);

        final var results = SimulationDriver.simulate(this.adaptation,
        schedule,
        Instant.now(),
        simulationDuration);

        lastResults = results;
        handleSimulationResults(lastResults);

    }

    /**
     * Fetches the resource schemas from the adaptation
     * @return a map from resource name to valueschema
     */
    private Map<String, ValueSchema> getResourceSchemas() {
        final class SchemaGetter<T> implements ResourceSolver.ApproximatorVisitor<T, ValueSchema> {
            @Override
            public ValueSchema real(final RealApproximator<T> approximator) {
                return ValueSchema.REAL;
            }

            @Override
            public ValueSchema discrete(final DiscreteApproximator<T> approximator) {
                return approximator.getSchema();
            }
        }

        final var schemas = new HashMap<String, ValueSchema>();

        for (final var family : this.adaptation.getResourceFamilies()) {
            final var schema = family.getSolver().approximate(new SchemaGetter<>());

            for (final var name : family.getResources().keySet()) {
                schemas.put(name, schema);
            }
        }

        return schemas;
    }

    /**
     * Updates resource feeders with results from a simulation.
     * @param results
     */
    private void handleSimulationResults(SimulationResults results){

        var sc  = getResourceSchemas();
        nameToType = new HashMap<String, String>();

        for(var schema : sc.entrySet()){
            var type = schema.getValue().match(new ValueSchema.Visitor<String>() {

                @Override
                public String onReal() {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, DOUBLE);
                    if(!feedersDouble.containsKey(nameRes)) {
                        feedersDouble.put(nameRes, new SimResource<Double>());
                    }
                    return "Double";
                }

                @Override
                public String onInt() {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, INTEGER);
                    if(!feedersInt.containsKey(nameRes)) {
                        feedersInt.put(nameRes, new SimResource<Integer>());
                    }
                    return "Integer";
                }

                @Override
                public String onBoolean() {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, BOOLEAN);
                    if(!feedersBool.containsKey(nameRes)) {
                        feedersBool.put(nameRes, new SimResource<Boolean>());
                    }
                    return "Boolean";
                }

                @Override
                public String onString() {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, STRING);
                    if(feedersString.containsKey(nameRes)) {
                        feedersString.put(nameRes, new SimResource<String>());
                    }
                    return "String";
                }

                @Override
                public String onDuration() {
                    unsupportedResources.add(schema.getKey());
                    return "Other";
                }

                @Override
                public String onPath() {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, STRING);
                    if(!feedersString.containsKey(nameRes)) {
                        feedersString.put(nameRes, new SimResource<String>());
                    }
                    return "String";
                }

                @Override
                public String onSeries(ValueSchema value) {
                    unsupportedResources.add(schema.getKey());
                    return "Other";
                }

                @Override
                public String onStruct(Map<String, ValueSchema> value) {
                    unsupportedResources.add(schema.getKey());
                    return "Other";
                }

                @Override
                public String onVariant(List<ValueSchema.Variant> variants) {
                    String nameRes = schema.getKey();
                    nameToType.put(nameRes, STRING);
                    if(!feedersString.containsKey(nameRes)) {
                        feedersString.put(nameRes, new SimResource<String>());
                    }
                    return "String";
                }
            });

        }
       for(Map.Entry<String, List<Pair<Duration, SerializedValue>>> entry: results.resourceSamples.entrySet()){
            String name = entry.getKey();
            String type = nameToType.get(entry.getKey());
            if(!unsupportedResources.contains(name)) {
                switch (type) {
                    case INTEGER -> getIntResource(name).initFromSimRes(deserialize(entry.getValue(), new IntegerValueMapper()), this.planningHorizon.getMinimum());
                    case BOOLEAN -> getBooleanResource(name).initFromSimRes(deserialize(entry.getValue(), new BooleanValueMapper()), this.planningHorizon.getMinimum());
                    case DOUBLE -> getDoubleResource(name).initFromSimRes(deserialize(entry.getValue(), new DoubleValueMapper()), this.planningHorizon.getMinimum());
                    case STRING -> getStringResource(name).initFromSimRes(deserialize(entry.getValue(), new StringValueMapper()), this.planningHorizon.getMinimum());
                    default -> throw new IllegalArgumentException("Not supported");
                }
            }
       }
    }

    private <T> List<Pair<Duration, T>> deserialize(List<Pair<Duration, SerializedValue>> values, ValueMapper<T> mapper){
        List<Pair<Duration, T>> deserialized = new ArrayList<Pair<Duration, T>>();
        for(var el : values){
            var des = mapper.deserializeValue(el.getValue()).getSuccessOrThrow();
            deserialized.add(Pair.of(el.getKey(), des));
        }
        return deserialized;
    }

    private SerializedValue serialize(Object paramValue){
        if(paramValue instanceof String){
            return SerializedValue.of((String)paramValue);
        }
        if(paramValue instanceof Enum){
            return SerializedValue.of(((Enum) paramValue).name());
        }
        if(paramValue instanceof Long){
            return SerializedValue.of((Long)paramValue);
        }
        if(paramValue instanceof Double){
            return SerializedValue.of((Double)paramValue);
        }
        if(paramValue instanceof Boolean){
            return SerializedValue.of((Boolean)paramValue);
        }
        return null;
    }


    // Resource feeders, mapping resource names to their corresponding resource accessor resulting from simulation results
    private final Map<String, SimResource<Integer>> feedersInt;
    private final Map<String, SimResource<Double>> feedersDouble;
    private final Map<String, SimResource<Boolean>> feedersBool;
    private final Map<String, SimResource<String>> feedersString;

    private Adaptation<?,?> adaptation;

    // planning horizon
    private Range<Time> planningHorizon;

    // local types for resources
    final private static String STRING = "String";
    final private static String INTEGER = "Integer";
    final private static String DOUBLE = "Double";
    final private static String BOOLEAN = "Boolean";

    // maps resource names to their local type
    private Map<String, String> nameToType;

    // stores the names of resources unsupported by the simulation facade due to type conversion
    private Set<String> unsupportedResources = new HashSet<String>();

    //simulation results from the last simulation
    private SimulationResults lastResults;


}
