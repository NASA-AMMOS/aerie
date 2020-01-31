package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.EventApplier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.Registry;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public class DataSystemModel implements SystemModel{

    private DataModelSlice initialSlice;
    private MissionModelGlue glue;
    private Registry registry;
    public EventApplier eventApplier;

    public DataSystemModel(MissionModelGlue glue, Instant initialTime){
        this.glue = glue;
        registry = glue.registry();
        eventApplier = glue.eventApplier();
        registerSelf();
        initialSlice = new DataModelSlice(initialTime);
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
        return this.initialSlice.cloneSlice();
    }

    @Override
    public void step(Slice aSlice, Duration dt) {
        DataModelSlice slice = (DataModelSlice) aSlice;
        slice.dataVolume += slice.dataRate * (double)(dt.durationInMicroseconds / 1000000L);
        if (slice.dataRate > 100){
            slice.dataProtocol = GlobalPronouns.spacewire;
        }
    }

    @Override
    public void registerSelf() {
        registry.provideSettable(this, GlobalPronouns.dataRate, Double.class, this::setDataRate, this::getDataRate);
        registry.provideSettable(this, GlobalPronouns.dataProtocol, String.class, this::setDataProtocol, this::getDataProtocol);
        registry.provideSettable(this, GlobalPronouns.dataVolume, Double.class, this::setDataVolume, this::getDataVolume);
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

    /* Need to create a cumulative event interface
    public void incrementDataRate(Slice slice, double delta){
        ((DataModelSlice)slice).dataRate += delta;
    }

    public void decrementDataRate(Slice slice,  double delta){
        ((DataModelSlice)slice).dataRate -= delta;
    }*/

    private static class DataModelSlice implements Slice{
        private double dataRate = 0.0;
        private double dataVolume = 0.0;
        private String dataProtocol = GlobalPronouns.UART;
        private Instant time;

        public DataModelSlice(Instant time){
            this.time = time;
        }

        //for now, time in constructor, will remove later
        public DataModelSlice(double dataRate, double dataVolume, String dataProtocol, Instant time){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
            this.dataProtocol = dataProtocol;
            this.time = time;
        }

        public DataModelSlice newSlice(double dataRate, double dataVolume, String dataProtocol, Instant time){
            return new DataModelSlice(dataRate, dataVolume, dataProtocol, time);
        }

        @Override
        public Instant time(){
            return this.time;
        }

        @Override
        public void setTime(Instant time){
            this.time = time;
        }

        public DataModelSlice cloneSlice(){
            return new DataModelSlice(dataRate, dataVolume, dataProtocol, time);
        }

        //will be used for review meeting then removed
        public void printSlice(){
            System.out.println("dataRate: " + dataRate + "\t" + " dataVolume: " + dataVolume + "\t" + " time: " + time);
        }
    }
}
