package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class DataModel {
    public enum Protocol {
        Spacewire,
        UART,
    }

    private Instant now;

    private final TreeMap<Instant, Double> dataRateHistory;
    private final TreeMap<Instant, Protocol> dataProtocolHistory;
    private double dataVolume;

    public DataModel(final Instant startTime) {
        this.now = startTime;
        this.dataRateHistory = new TreeMap<>();
        this.dataProtocolHistory = new TreeMap<>();
        this.dataVolume = 0.0;

        this.dataRateHistory.put(this.now, 0.0);
        this.dataProtocolHistory.put(this.now, Protocol.UART);
    }

    public DataModel(final DataModel other) {
        this.now = other.now;
        this.dataRateHistory = new TreeMap<>(other.dataRateHistory);
        this.dataProtocolHistory = new TreeMap<>(other.dataProtocolHistory);
        this.dataVolume = other.dataVolume;
    }

    public void step(final Duration delta){
        this.now = this.now.plus(delta);
        this.dataVolume += this.dataRateHistory.lastEntry().getValue() * delta.asIntegerQuantity(TimeUnit.SECONDS);
    }

    public void accumulateDataRate(final double delta) {
        this.dataRateHistory.put(this.now, this.dataRateHistory.lastEntry().getValue() + delta);

        if (this.dataRateHistory.lastEntry().getValue() > 100
            && !Objects.equals(this.dataProtocolHistory.lastEntry().getValue(), Protocol.Spacewire))
        {
            setDataProtocol(Protocol.Spacewire);
        }
    }

    public void setDataVolume(final double volume) {
        this.dataVolume = volume;
    }

    public void setDataProtocol(final Protocol protocol) {
        this.dataProtocolHistory.put(this.now, protocol);
    }


    public double getDataRate(){
        return this.dataRateHistory.lastEntry().getValue();
    }

    public double getDataVolume(){
        return this.dataVolume;
    }

    public Protocol getDataProtocol(){
        return this.dataProtocolHistory.lastEntry().getValue();
    }


    public Map<Instant, Double> getDataRateHistory() {
        return new TreeMap<>(this.dataRateHistory);
    }

    public Map<Instant, Protocol> getDataProtocolHistory() {
        return new TreeMap<>(this.dataProtocolHistory);
    }


    public List<Window> whenRateGreaterThan(final double threshold) {
        final var windows = new ArrayList<Window>();

        final var iter = this.dataRateHistory.entrySet().iterator();
        while (iter.hasNext()) {
            Instant start = null;
            while (iter.hasNext()) {
                final var point = iter.next();
                if (point.getValue() > threshold) {
                    start = point.getKey();
                    break;
                }
            }
            if (start == null) break;

            Instant end = null;
            while (iter.hasNext()) {
                final var point = iter.next();
                if (point.getValue() <= threshold) {
                    end = point.getKey();
                    break;
                }
            }
            if (end == null) end = now;

            windows.add(Window.between(start, end));
        }

        return windows;
    }
}
