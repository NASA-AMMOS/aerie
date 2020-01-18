package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.ArrayList;
import java.util.List;

public class DataSystemModel implements SystemModel, MissionModelGlue {

    private double dataRate = 10.0;
    private double dataVolume = 0.0;
    private String dataProtocol = GlobalPronouns.UART;

    public DataSystemModel(){
        registerSelf();
        slices.add(new DataModelSlice(dataRate, dataVolume, dataProtocol));
    }
    //this shouldn't be here
    //cache these elsewhere
    //who owns list of cached slices?
    private List<DataModelSlice> slices = new ArrayList<>();

    @Override
    public void step(Slice slice, Duration dt) {
        applySlice(slice);
        dataVolume = dataRate*dt.totalSeconds();
        if (dataRate > 100){
            dataProtocol = GlobalPronouns.spacewire;
        }
        saveToSlice();
    }


    @Override
    public void registerSelf() {
        Registry.provide(this, GlobalPronouns.dataRate, this::getDataRate);
        Registry.provide(this, GlobalPronouns.dataVolume, this::getDataRate);
    }

    public void saveToSlice(){
        DataModelSlice slice = new DataModelSlice(this.dataRate, this.dataVolume, this.dataProtocol);
        slices.add(slice);
    }

    //probably should move this into Slice interface but then we need to probably also implement a DataModelSlice interface
    public void applySlice(Slice slice){
        DataModelSlice temp = (DataModelSlice) slice;
        this.dataRate = temp.dataRate;
        this.dataVolume = temp.dataVolume;
        this.dataRate = temp.dataRate;
    }

    public DataModelSlice latestSlice(){
        return slices.get(slices.size()-1);
    }

    //not sure if this is the best way to get something
    public double getDataRate(){
        return this.latestSlice().dataRate;
    }

    public double getDataVolume(){
        return this.latestSlice().dataVolume;
    }

    public String getDataProtocol(){
        return this.latestSlice().dataProtocol;
    }

    //using public modifiers on variables b/c it may be a pain to reimplement another set of getters and setters
    private static class DataModelSlice implements Slice{
        public double dataRate = 0.0;
        public double dataVolume = 0.0;
        public String dataProtocol = GlobalPronouns.UART;
        public Time time;

        public DataModelSlice(double dataRate, double dataVolume, String dataProtocol){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
            this.dataProtocol = dataProtocol;
        }
    }
}
