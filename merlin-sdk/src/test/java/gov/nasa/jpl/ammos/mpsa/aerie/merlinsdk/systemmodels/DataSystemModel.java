package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.RegexUtilities;

import java.util.function.Function;


public class DataSystemModel implements SystemModel, MissionModelGlue {

    public DataSystemModel(){
        registerSelf();
        latestSlice = new DataModelSlice();
    }

    private DataModelSlice latestSlice;

    public Registry registry;

    public Registry getRegistry(){
        return this.registry;
    }

    public void setRegistry(Registry registry){
        this.registry = registry;
    }

    public Slice getSlice(){
        return this.latestSlice;
    }



    //this modifies a slice given to is, so we don't have to return a slice necessarily
    //we can return the same slice passed in as a parameter if that makes more ergonomic sense

    public DataModelSlice step2(DataModelSlice slice, Duration dt) {
        double dataVolume = slice.dataRate * dt.totalSeconds();
        String dataProtocol = slice.dataProtocol;
        if (slice.dataRate > 100){
            dataProtocol = GlobalPronouns.spacewire;
        }
        DataModelSlice nextSlice = slice.newSlice(slice.dataRate, dataVolume, dataProtocol);
        this.latestSlice = nextSlice;
        return this.latestSlice;
    }

    public DataModelSlice latestSlice(){
        return this.latestSlice;
    }



    @Override
    public DataModelSlice step(DataModelSlice slice, Duration dt) {
        slice.dataVolume = slice.dataRate * dt.totalSeconds();
        if (slice.dataRate > 100){
            slice.dataProtocol = GlobalPronouns.spacewire;
        }
        return slice.clone();
    }

    @Override
    public void registerSelf() {
       registry.registerGetter(this, GlobalPronouns.dataRate, Double.class, this::getDataRate);
       registry.registerSetter(this, GlobalPronouns.dataRate, this::setDataRate);



        /*
        Registry.provide(this, GlobalPronouns.dataRate, this::getDataRate);
        Registry.provide(this, GlobalPronouns.dataVolume, this::getDataVolume);
        Registry.provide(this, GlobalPronouns.dataProtocol, this::getDataProtocol);

        Registry.provideDouble(this, GlobalPronouns.dataRate, this::setDataRate);
        Registry.provideDouble(this, GlobalPronouns.dataVolume, this::setDataVolume);
        Registry.provideString(this, GlobalPronouns.dataProtocol, this::setDataProtocol);*/

        //provideSettable, provideCumulative
        //Registry.provideCumulative(this::incrementDataRate);
        //mission modeler only needs to look at one place to make sure that the provide related to the state they want is here
        //getters and setters should have a slice parameter

    }

    @Override
    public void applySlice(Slice slice) {

    }

    @Override
    public Slice saveToSlice() {
        return null;
    }

    //not sure if this is the best way to get something
    public double getDataRate(DataModelSlice slice){
        return slice.dataRate;
    }

    public double getDataVolume(DataModelSlice slice){
        return slice.dataVolume;
    }

    public String getDataProtocol(DataModelSlice slice){
        return slice.dataProtocol;
    }

    public void setDataRate(DataModelSlice slice, Double dataRate){
        slice.dataRate = dataRate;
    }

    public void setDataVolume(DataModelSlice slice,  Double dataVolume){
        slice.dataVolume = dataVolume;
    }

    public void setDataProtocol(DataModelSlice slice,  String dataProtocol){
        slice.dataProtocol = dataProtocol;
    }

    public void incrementDataRate(DataModelSlice slice, double delta){
        slice.dataRate += delta;
    }

    public void decrementDataRate(DataModelSlice slice,  double delta){
        slice.dataRate -= delta;
    }

    private static class DataModelSlice implements Slice{
        private double dataRate = 0.0;
        private double dataVolume = 0.0;
        private String dataProtocol = GlobalPronouns.UART;
        private Time time;

        public DataModelSlice(){}

        public DataModelSlice(double dataRate, double dataVolume, String dataProtocol){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
            this.dataProtocol = dataProtocol;
        }

        public DataModelSlice newSlice(double dataRate, double dataVolume, String dataProtocol){
            return new DataModelSlice(dataRate, dataVolume, dataProtocol);
        }

        public DataModelSlice clone(){
            return new DataModelSlice(this.dataRate, this.dataVolume, this.dataProtocol);
        }

        public Time time(){
            return this.time;
        }


    }
}
