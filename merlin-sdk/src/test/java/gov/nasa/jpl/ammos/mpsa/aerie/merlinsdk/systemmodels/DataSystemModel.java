package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public class DataSystemModel implements SystemModel<DataSystemModel.DataModelSlice> {
    public DataSystemModel(){}

    @Override
    public DataModelSlice getInitialSlice(){
        return new DataModelSlice();
    }

    @Override
    public void registerResources(final ResourceRegistrar<DataModelSlice> registrar){
        registrar.subscribe(GlobalPronouns.dataRate, AccumulateStimulus.class, (slice, stimulus) -> {
            slice.accumulateDataRate(stimulus.getDelta(Double.class));
        });
        registrar.subscribe(GlobalPronouns.dataVolume, SetStimulus.class, (slice, stimulus) -> {
            slice.setDataVolume(stimulus.getNewValue(Double.class));
        });
        registrar.subscribe(GlobalPronouns.dataProtocol, SetStimulus.class, (slice, stimulus) -> {
            slice.setDataProtocol(stimulus.getNewValue(String.class));
        });

        registrar.provideResource(GlobalPronouns.dataRate, Double.class, DataModelSlice::getDataRate);
        registrar.provideResource(GlobalPronouns.dataVolume, Double.class, DataModelSlice::getDataVolume);
        registrar.provideResource(GlobalPronouns.dataProtocol, String.class, DataModelSlice::getDataProtocol);
    }

    public static class DataModelSlice implements Slice{
        private double dataRate = 0.0;
        private double dataVolume = 0.0;
        private String dataProtocol = GlobalPronouns.UART;

        private DataModelSlice(){}

        public DataModelSlice(final DataModelSlice other) {
            this.dataRate = other.dataRate;
            this.dataVolume = other.dataVolume;
            this.dataProtocol = other.dataProtocol;
        }

        @Override
        public DataModelSlice duplicate(){
            return new DataModelSlice(this);
        }

        @Override
        public void step(final Duration delta){
            this.dataVolume += this.dataRate * delta.asIntegerQuantity(TimeUnit.SECONDS);
            if (this.dataRate > 100){
                this.dataProtocol = GlobalPronouns.spacewire;
            }
        }

        public double getDataRate(){
            return this.dataRate;
        }

        public void accumulateDataRate(final double delta){
            this.dataRate += delta;
        }


        public double getDataVolume(){
            return this.dataVolume;
        }

        public void setDataVolume(final double dataVolume){
            this.dataVolume = dataVolume;
        }


        public String getDataProtocol(){
            return this.dataProtocol;
        }

        public void setDataProtocol(final String dataProtocol){
            this.dataProtocol = dataProtocol;
        }
    }
}
