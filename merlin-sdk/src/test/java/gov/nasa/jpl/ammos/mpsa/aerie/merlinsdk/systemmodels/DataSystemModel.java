package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

public class DataSystemModel implements SystemModel<DataSystemModel.DataModelSlice> {
    public DataSystemModel(){}

    @Override
    public DataModelSlice getInitialSlice(){
        return new DataModelSlice();
    }

    public static final class DataModelSlice implements Slice{
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
        }

        @Override
        public void react(final String resourceName, final Stimulus stimulus){
            switch (resourceName){
                case GlobalPronouns.dataRate: {
                    this.dataRate += ((AccumulateStimulus)stimulus).getDelta(Double.class);
                    if (this.dataRate > 100) {
                        this.dataProtocol = GlobalPronouns.spacewire;
                    }
                    break;
                }

                case GlobalPronouns.dataVolume: {
                    this.dataVolume = ((SetStimulus)stimulus).getNewValue(Double.class);
                    break;
                }

                case GlobalPronouns.dataProtocol: {
                    this.dataProtocol = ((SetStimulus)stimulus).getNewValue(String.class);
                    break;
                }
            }
        }

        public double getDataRate(){
            return this.dataRate;
        }

        public double getDataVolume(){
            return this.dataVolume;
        }

        public String getDataProtocol(){
            return this.dataProtocol;
        }
    }
}
