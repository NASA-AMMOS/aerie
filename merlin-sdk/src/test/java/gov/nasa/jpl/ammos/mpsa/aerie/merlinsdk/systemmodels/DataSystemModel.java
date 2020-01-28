package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.EventApplier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.Registry;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;


public class DataSystemModel implements SystemModel{

    private DataModelSlice initialSlice;
    private MissionModelGlue glue;
    private Registry registry;
    public EventApplier eventApplier;

    public DataSystemModel(MissionModelGlue glue){
        this.glue = glue;
        registerSelf();
        initialSlice = new DataModelSlice();
    }

    @Override
    public Registry getRegistry(){
        return this.glue.registry();
    }

    @Override
    public EventApplier getEventAplier() {
        return this.glue.eventApplier();
    }

    @Override
    public Slice getInitialSlice(){
        return this.initialSlice;
    }

    @Override
    public void step(Slice aSlice, Duration dt) {
        DataModelSlice slice = (DataModelSlice) aSlice;
        slice.dataVolume = slice.dataRate * dt.totalSeconds();
        if (slice.dataRate > 100){
            slice.dataProtocol = GlobalPronouns.spacewire;
        }
    }

    @Override
    public void registerSelf() {
        registry.registerGetter(this, GlobalPronouns.dataRate, Double.class, this::getDataRate);
        registry.registerSetter(this, GlobalPronouns.dataRate, Double.class, this::setDataRate);
    }

    public double getDataRate(Slice slice){
        return ((DataModelSlice)slice).dataRate;
    }

    public double getDataVolume(Slice slice){
        return ((DataModelSlice)slice).dataVolume;
    }

    public String getDataProtocol(Slice slice){
        return ((DataModelSlice)slice).dataProtocol;
    }

    public void setDataRate(Slice slice, Double dataRate){
        ((DataModelSlice)slice).dataRate = dataRate;
    }

    public void setDataVolume(Slice slice,  Double dataVolume){
        ((DataModelSlice)slice).dataVolume = dataVolume;
    }

    public void setDataProtocol(Slice slice,  String dataProtocol){
        ((DataModelSlice)slice).dataProtocol = dataProtocol;
    }

    public void incrementDataRate(Slice slice, double delta){
        ((DataModelSlice)slice).dataRate += delta;
    }

    public void decrementDataRate(Slice slice,  double delta){
        ((DataModelSlice)slice).dataRate -= delta;
    }

    private static class DataModelSlice implements Slice{
        private double dataRate = 0.0;
        private double dataVolume = 0.0;
        private String dataProtocol = GlobalPronouns.UART;
        private Time time;

        public DataModelSlice(){}

        //for now, time in constructor, will remove later
        public DataModelSlice(double dataRate, double dataVolume, String dataProtocol, Time time){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
            this.dataProtocol = dataProtocol;
            this.time = time;
        }

        public DataModelSlice newSlice(double dataRate, double dataVolume, String dataProtocol, Time time){
            return new DataModelSlice(dataRate, dataVolume, dataProtocol, time);
        }

        public Time time(){
            return this.time;
        }
    }
}
