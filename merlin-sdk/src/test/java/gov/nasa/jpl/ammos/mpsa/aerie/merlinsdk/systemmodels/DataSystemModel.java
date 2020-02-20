package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.MasterSystemModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.Registry;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.List;

public class DataSystemModel implements SystemModel{

    private DataModelSlice initialSlice;
    private MissionModelGlue glue;
    private Registry registry;
    private String name = GlobalPronouns.DataSystemModel;

    public DataSystemModel(MissionModelGlue glue, Instant initialTime){
        this.glue = glue;
        registry = glue.registry();
        registerSelf();
        initialSlice = new DataModelSlice(initialTime);
    }

    @Override
    public Registry getRegistry(){
        return this.glue.registry();
    }

    @Override
    public MasterSystemModel getMasterSystemModel() { return this.glue.MasterSystemModel(); }

    @Override
    public Slice getInitialSlice(){
        return this.initialSlice.cloneSlice();
    }

    @Override
    public void mapStateNameToSystemModelName(String stateName){
        this.glue.mapStateNameToSystemModelName(stateName, this.name);
    }


    @Override
    public void step(Slice aSlice, Duration dt) {
        DataModelSlice slice = (DataModelSlice) aSlice;
        slice.dataVolume += slice.dataRate * (double)(dt.durationInMicroseconds / 1000000L);
        if (slice.dataRate > 100){
            slice.dataProtocol = GlobalPronouns.spacewire;
        }
        slice.setTime(slice.time().plus(dt));
    }

    @Override
    public void registerSelf() {
        registry.provideSettable(this, GlobalPronouns.dataRate, Double.class, this::setDataRate, this::getDataRate);
        registry.provideSettable(this, GlobalPronouns.dataProtocol, String.class, this::setDataProtocol, this::getDataProtocol);
        registry.provideSettable(this, GlobalPronouns.dataVolume, Double.class, this::setDataVolume, this::getDataVolume);
    }

    @Override
    public String getName(){
        return this.name;
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

    public List<Window> whenDataRateLessThan(double x, DataModelSlice slice){
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for data rate less than " + x);
        return windows;
    }

    public List<Window> whenDataRateGreaterThan(double x){
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for data rate greater than than " + x);
        return windows;
    }

    public List<Window> whenDataVolumeGreaterThan(double x){
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for data volume greater than than " + x);
        return windows;
    }

    public List<Window> whenDataProtocol(String s){
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for when data protocol is " + s);
        return windows;
    }

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
