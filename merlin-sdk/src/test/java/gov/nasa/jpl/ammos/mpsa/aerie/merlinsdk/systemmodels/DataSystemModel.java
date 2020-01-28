package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.*;


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

    public Registry getRegistry(){
        return this.glue.registry();
    }

    public EventApplier getEventApplier(){
        return this.glue.eventApplier();
    }

    public Slice getInitialSlice(){
        return this.initialSlice;
    }

    public void step(DataModelSlice slice, Duration dt) {
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
