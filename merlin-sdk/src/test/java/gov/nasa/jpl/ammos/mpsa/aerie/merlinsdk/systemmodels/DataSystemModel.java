package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class DataSystemModel implements Slice {
    private Instant now;
    private final Map<Instant, Double> dataRateHistory;
    private final Map<Instant, String> dataProtocolHistory;

    private double dataRate;
    private double dataVolume;
    private String dataProtocol;

    public DataSystemModel(final Instant startTime) {
        this.now = startTime;
        this.dataRateHistory = new TreeMap<>();
        this.dataProtocolHistory = new TreeMap<>();
        this.dataRate = 0.0;
        this.dataVolume = 0.0;
        this.dataProtocol = GlobalPronouns.UART;

        this.dataRateHistory.put(this.now, this.dataRate);
        this.dataProtocolHistory.put(this.now, this.dataProtocol);
    }

    public DataSystemModel(final DataSystemModel other) {
        this.now = other.now;
        this.dataRateHistory = new TreeMap<>(other.dataRateHistory);
        this.dataProtocolHistory = new TreeMap<>(other.dataProtocolHistory);
        this.dataRate = other.dataRate;
        this.dataVolume = other.dataVolume;
        this.dataProtocol = other.dataProtocol;
    }

    @Override
    public DataSystemModel duplicate(){
        return new DataSystemModel(this);
    }

    @Override
    public void step(final Duration delta){
        this.now = this.now.plus(delta);
        this.dataVolume += this.dataRate * delta.asIntegerQuantity(TimeUnit.SECONDS);
    }

    @Override
    public void react(final String resourceName, final Stimulus stimulus){
        switch (resourceName){
            case GlobalPronouns.dataRate:
                this.accumulateDataRate(((AccumulateStimulus)stimulus).getDelta(Double.class));
                break;

            case GlobalPronouns.dataVolume:
                this.setDataVolume(((SetStimulus)stimulus).getNewValue(Double.class));
                break;

            case GlobalPronouns.dataProtocol:
                this.setDataProtocol(((SetStimulus)stimulus).getNewValue(String.class));
                break;
        }
    }

    public void accumulateDataRate(final double delta) {
        this.dataRate += delta;
        this.dataRateHistory.put(this.now, this.dataRate);

        if (this.dataRate > 100 && !Objects.equals(this.dataProtocol, GlobalPronouns.spacewire)) {
            setDataProtocol(GlobalPronouns.spacewire);
        }
    }

    public void setDataVolume(final double volume) {
        this.dataVolume = volume;
    }

    public void setDataProtocol(final String protocol) {
        this.dataProtocol = protocol;
        this.dataProtocolHistory.put(this.now, this.dataProtocol);
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

    public Map<Instant, Double> getDataRateHistory() {
        return new TreeMap<>(this.dataRateHistory);
    }

    public Map<Instant, String> getDataProtocolHistory() {
        return new TreeMap<>(this.dataProtocolHistory);
    }
}
