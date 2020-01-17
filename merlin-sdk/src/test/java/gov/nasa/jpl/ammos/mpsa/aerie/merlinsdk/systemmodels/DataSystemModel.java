package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

public class DataSystemModel implements SystemModel, MissionModelGlue {

    private double dataRate = 0.0;
    private double dataVolume = 0.0;

    //this shouldn't be here
    //cache these elsewhere
    private DataModelSlice latestSlice = new DataModelSlice(dataRate, dataVolume);

    @Override
    public void step(Slice slice) {

    }

    @Override
    public void registerSelf() {
        Registry.provide(this, "data rate", this::getDataRate);
        Registry.provide(this, "data volume", this::getDataRate);
    }

    //not sure if this is the best way to get something
    public double getDataRate(){
        return this.latestSlice.dataRate;
    }

    public double getDataVolume(){
        return this.latestSlice.dataVolume;
    }

    //using public modifiers on variables b/c it may be a pain to reimplement another set of getters and setters
    private static class DataModelSlice implements Slice{
        public double dataRate = 0.0;
        public double dataVolume = 0.0;

        public DataModelSlice(double dataRate, double dataVolume){
            this.dataRate = dataRate;
            this.dataVolume = dataVolume;
        }
    }
}
