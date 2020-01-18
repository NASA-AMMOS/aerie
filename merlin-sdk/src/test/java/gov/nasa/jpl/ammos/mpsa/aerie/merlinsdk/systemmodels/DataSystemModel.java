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
    public void step(Duration dt) {
        dataVolume = dataRate*dt.totalSeconds();
        if (dataRate > 100){
            dataProtocol = GlobalPronouns.spacewire;
        }
    }


    @Override
    public void registerSelf() {
        Registry.provide(this, GlobalPronouns.dataRate, this::getDataRate);
        Registry.provide(this, GlobalPronouns.dataVolume, this::getDataVolume);
        Registry.provide(this, GlobalPronouns.dataProtocol, this::getDataProtocol);

        Registry.provideDouble(this, GlobalPronouns.dataRate, this::setDataRate);
        Registry.provideDouble(this, GlobalPronouns.dataVolume, this::setDataVolume);
        Registry.provideString(this, GlobalPronouns.dataProtocol, this::setDataProtocol);
    }

    @Override
    public Slice saveToSlice(){
        DataModelSlice slice = new DataModelSlice(this.dataRate, this.dataVolume, this.dataProtocol);
        slices.add(slice);
        return slice;
    }

    //probably should move this into Slice interface but then we need to probably also implement a DataModelSlice interface
    @Override
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

    public void setDataRate(Double dataRate){
        this.dataRate = dataRate;
    }

    public void setDataVolume(Double dataVolume){
        this.dataVolume = dataVolume;
    }

    public void setDataProtocol(String dataProtocol){
        this.dataProtocol = dataProtocol;
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
