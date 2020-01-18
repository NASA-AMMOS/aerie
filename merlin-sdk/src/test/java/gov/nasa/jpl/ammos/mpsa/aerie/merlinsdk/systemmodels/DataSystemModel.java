package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

public class DataSystemModel implements SystemModel, MissionModelGlue {

    private double dataRate = 10.0;
    private double dataVolume = 0.0;
    private String dataProtocol = GlobalPronouns.UART;

    public DataSystemModel(){
        registerSelf();
    }

    //this shouldn't be here
    //cache these elsewhere
    //who owns list of cached slices?
    private DataModelSlice latestSlice = new DataModelSlice(dataRate, dataVolume);

    @Override
    public void step(Slice slice) {

    }

    @Override
    public void registerSelf() {
        Registry.provide(this, GlobalPronouns.dataRate, this::getDataRate);
        Registry.provide(this, GlobalPronouns.dataVolume, this::getDataRate);
    }

    //not sure if this is the best way to get something
    public double getDataRate(){
        return this.latestSlice.dataRate;
    }

    public double getDataVolume(){
        return this.latestSlice.dataVolume;
    }

    public String getDataProtocol(){
        return this.latestSlice.dataProtocol;
    }

    //using public modifiers on variables b/c it may be a pain to reimplement another set of getters and setters
    private static class DataModelSlice implements Slice{
        public double dataRate = 0.0;
        public double dataVolume = 0.0;
        public String dataProtocol = GlobalPronouns.UART;

        public DataModelSlice(double dataRate, double dataVolume){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
        }
    }
}
