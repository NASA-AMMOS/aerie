package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.LinkedHashMap;
import java.util.Map;

public class InstrumentModel implements SettableState<Double> {

    private String name;

    private Double rate;

    private String dataProtocol;

    //Question: by having this as a member of the instrument, the instrument can then trigger the bin model
    //to update itself.  I don't love this sort of composition, and other alternatives I have thought of are
    //change listeners or a bus.  Jonathan also very helpfully created a sample using an inner class approach,
    // but I feel like this makes the parent class unwieldy.  It also suggests a relationship between the
    // child and parent class that might not always be what the adapter wants.
    private BinModel bin;

    private SimulationEngine engine;

    private Map<Time, Double> stateHistory = new LinkedHashMap<>();

    //a planner may only care when an instrument is on or off
    //this would be a good derived state to make later
    private boolean on_status = false;

    public void setDataProtocol(String dataProtocol){
        this.dataProtocol = dataProtocol;
    }

    public String getDataProtocol() { return this.dataProtocol; }

    public InstrumentModel(String name, Double rate){
        this.name = name;
        this.rate = rate;
        if (this.rate > 0){
            this.on_status = true;
        }
    }

    //TODO: Add a constructor in the bin model that doesn't have an instrument, this makes less sense
    public InstrumentModel(String name, Double rate, BinModel bin){
        this.name = name;
        this.rate = rate;
        if (this.rate > 0){
            this.on_status = true;
        }
        this.bin = bin;
    }

    /** If everyone is OK w/ me adding getName() to the state interface I will remove this comment block.


    // I need this b/c in my OnboardDataModelStates constructor I use the name to add each state to a map.
    // B/c I'm cycling thru all the States, I'm using the generic State object and need a method that the State object has
    // I'm guessing it's not kosher to override the toString method to return a name/identifier
    //I think I should probably make a OnboardData state interface and this implement it
    //Or I can just create a getName method in both data related classes and then just force the adapter to specify which
    //implemented state class they are using
    //I'll fix this before PR
    @Override
    public String toString(){
        return this.name;
    }*/

    @Override
    public String getName() { return this.name; }

    public void turnOn(double rate){
        this.set(rate);
    }

    public void turnOff(){
        this.set(0.0);
    }

    public boolean onStatus(){
        return this.on_status;
    }

    public void setBin(BinModel bin){
        this.bin = bin;
    }

    public String getBinName() {return this.bin.toString(); }

    //I'm adding this method b/c get rate makes more sense than get, InstrumentModel.get() is vague about what represented value is being returned
    //an InstrumentModel represents more than just the rate so renaming it to InstrumentModelRate might be overly specific
    public Double getRate() { return this.get(); }

    //similar to getRate I added a setRate
    public void setRate(double rate) { this.set(rate); }

    @Override
    public Double get() {
        return rate;
    }

    @Override
    public void set(Double newRate){
        Time curTime = engine.getCurrentSimulationTime();
        bin.updateRate(curTime, newRate - this.rate);
        this.rate = newRate;
        if (this.rate > 0.0){
            this.on_status = true;
        }
        else {
            this.on_status = false;
        }
        stateHistory.put(curTime, this.rate);
    }

    @Override
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    @Override
    public Map<Time, Double> getHistory() {
        return stateHistory;
    }

    public void printHistory(){
        System.out.println("INSTRUMENT NAME : " + this.name);
        this.stateHistory.forEach((key, value) -> System.out.println("TIME: " + key + " : " + value));
        System.out.println();
    }

    //this method is just to help me copy and paste rows of data into Excel to see if the graphs look like I expect them too
    //I will remove it before the PR
    public void printHistoryGraphFormat(){
        System.out.println("INSTRUMENT NAME : " + this.name);
        System.out.println("times");
        for(Time x : this.stateHistory.keySet()){
            System.out.println(x.getMilliseconds());
        }
        System.out.println("values");
        for(Double x : this.stateHistory.values()){
            System.out.println(x);
        }
    }
}
