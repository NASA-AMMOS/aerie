package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.*;

public class DataSystemModel implements SystemModel{

    public DataSystemModel(MissionModelGlue glue){
        this.glue = glue;
        setRegistry();
        setEventApplier();
        registerSelf();
        latestSlice = new DataModelSlice();
    }

    public void setRegistry(){
        this.registry = this.glue.Registry;
    }

    public void setEventApplier(){
        this.eventApplier = this.glue.EventApplier;
    }

    private DataModelSlice latestSlice;
    private MissionModelGlue glue;
    private Registry registry;
    public EventApplier eventApplier;

    public MissionModelGlue getGlue() {
        return this.glue;
    }

    public Registry getRegistry(){
        return this.registry;
    }

    public EventApplier getEventApplier(){
        return this.
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
       registry.registerSetter(this, GlobalPronouns.dataRate, Double.class, this::setDataRate);
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
