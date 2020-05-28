package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

//In the future, we can have some integrating state interface and have this implement it
//This implements a state b/c I didn't want to implement the increment/decrement methods that come with
//CumulativeState.  Really this state is just applying a delta.  Which is another reason we should
//probably create the correct interface for this to implement.


//While there are many members of this state, the value being tracked by the state history is *data volume*
//If you want to see data rate, you can look at the instruments
//A future derived state to implement is total data rate for a bin model
//the other option is storing both data rate and data volume in the state history (e.g. as a tuple)...
public class BinModel implements State<Double> {

    private String binID;

    private double currentDataRate = 0;

    private double currentDataVolume = 0;

    private List<InstrumentDataRateModel> instruments = new ArrayList<>();

    private boolean scienceData = false;

    // I do this b/c you can't do a get(index) on a hashmap, and it takes O(n) to get to the last element
    private Instant lastUpdatedTime;

    // It would be nice if this auto-initialized to the current sim time when the sim first starts, e.g.:
    //  private Map<Time, java.lang.Double> stateHistory = new LinkedHashMap<>(){{
    //        put(engine.getCurrentSimulationTime(), 0.0);
    //    }};
    //
    // but there is no way for this state to know what that is until an effect model perturbs it.
    // that is why we have an initializeBinData() method that was created specificially for the effect model of
    // an initialize bin data activity to call. (See InitializeBinDataVolume activity for a continuation of these comments).
    // let me know if you have any ideas on how I can better do this.
    private Map<Instant, Double> stateHistory = new LinkedHashMap<>();

    @Override
    public void initialize(Instant startTime){
        stateHistory.put(startTime, this.currentDataRate);
        lastUpdatedTime = startTime;
    }


    public void updateVolumeAndHistory(Instant updateVolumeTime){

        //get the amount of time that has passed since the last time volume was updated
        Duration delta = updateVolumeTime.durationFrom(this.lastUpdatedTime);

        //the data rate is constant in this interval
        this.currentDataVolume += (delta.durationInMicroseconds / 1000000.0) * this.currentDataRate;

        //record this new data volume
        this.stateHistory.put(updateVolumeTime, currentDataVolume);

        //update the lastUpdatedTime
        this.lastUpdatedTime = updateVolumeTime;
    }


    public BinModel(String binID, InstrumentDataRateModel... instrumentDataRateModels){

        this.binID = binID;

        this.instruments = asList(instrumentDataRateModels);

        //sums the data rate of all the instruments writing to this bin
        for (InstrumentDataRateModel x: this.instruments){
            this.currentDataRate += x.get();
            x.setBin(this);
        }

        //At this point, it would be ideal to initialize the history with the value and the start time.
        //However, we can't b/c we don't have the current sim time, so if we have a non-0 value for the data-rate
        //the volume will not be calculated properly. This is why we need an initialize bin volume activity.
        //can't use: updateVolumeAndHistory(engine.getCurrentSimulationTime());
    }

    /* at some point, we may want to create a derived state where we can see what instruments write to what bin.
    I'm not sure if this is necessary now
    Also, if we do it by composition, it gets messy, since now an instrument has a bin, and a bin has a (more likely many) instruments
    public void addInstrument(InstrumentModel instrumentModel){
        instruments.add(instrumentModel);
    }*/

    //probably should create another method for this (see my notes in InstrumentModel)
    @Override
    public String toString(){
        return this.binID;
    }

    public void printHistory(){
        System.out.println("BIN ID : " + binID);
        this.stateHistory.forEach((key, value) -> System.out.println("TIME: " + key + " : " + value));
    }

    @Override
    public String getName(){ return this.binID; }


    public void downlink(){

        Instant curTime = SimulationEffects.now();

        //calculate all the volume that has been accumulated up until this point
        //this is stored in the history with the key as the current time
        updateVolumeAndHistory(curTime);

        //set the current data volume to 0 since all data has been downlinked
        this.currentDataVolume = 0.0;

        //increase the current time by the smallest step possible (1ms)
        //otherwise the time key will be the same as the previous entry (with max volume)
        Instant curTimePlusDeltaT = curTime.plus(1, TimeUnit.MICROSECONDS);

        //update the history
        stateHistory.put(curTimePlusDeltaT, this.currentDataVolume);
    }

    public void downlink(double amount){

        Instant curTime = SimulationEffects.now();

        //calculate all the volume that has been accumulated up until this point
        //this is stored in the history with the key as the current time
        updateVolumeAndHistory(curTime);

        //set the current data volume to 0 since all data has been downlinked
        //set an error if downlinked below an amountƒdece
        this.currentDataVolume -= amount;

        //increase the current time by the smallest step possible (1ms)
        //otherwise the time key will be the same as the previous entry (with max volume)
        Instant curTimePlusDeltaT = curTime.plus(1, TimeUnit.MICROSECONDS);

        //update the history
        stateHistory.put(curTimePlusDeltaT, this.currentDataVolume);
    }

    //whenever an instrument that writes data this this bin changes its data rate, this method should be called!
    //as a result, our data volume computation will always record at what point there is a slope change, and no slope changes will go unaccounted for
    public void updateRate(Instant timeRateUpdated, Double deltaRate){

        //integrate the volume up until when the rate was changed
        //we use the time the rate was updated as a parameter b/c I am not sure if the sim time will move forward or not as of yet
        updateVolumeAndHistory(timeRateUpdated);

        //update the data rate
        this.currentDataRate += deltaRate;
    }

    @Override
    public Double get() {
        //this get will add an entry to the state history, even though the data rate hasn't changed!
        //the added entry will just be a point on a line
        updateVolumeAndHistory(SimulationEffects.now());
        return this.currentDataVolume;
    }

    //just for me to graph data for now in excel
    @Override
    public Map<Instant, Double> getHistory() {
        return this.stateHistory;
    }

    public void printHistoryGraphFormat(){
        System.out.println("BIN ID : " + this.binID);
        System.out.println("times");
        for(Instant x : this.stateHistory.keySet()){
            System.out.println(x);
        }
        System.out.println("values");
        for(Double x : this.stateHistory.values()){
            System.out.println(x);
        }
    }


}
