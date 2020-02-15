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
    public enum Protocol { Spacewire, UART }

    private Instant now;

    private final Instant initialInstant;
    private final double initialDataVolume;
    private final TreeMap<Instant, Double> dataRateHistory;
    private final TreeMap<Instant, Protocol> dataProtocolHistory;

    public DataModel(final Instant startTime) {
        this.now = startTime;

        this.initialInstant = this.now;
        this.initialDataVolume = 0.0;
        this.dataRateHistory = new TreeMap<>();
        this.dataProtocolHistory = new TreeMap<>();

        this.dataRateHistory.put(this.now, 0.0);
        this.dataProtocolHistory.put(this.now, Protocol.UART);
    }

    public DataModel(final DataModel other) {
        this.now = other.now;

        this.initialInstant = other.initialInstant;
        this.initialDataVolume = other.initialDataVolume;
        this.dataRateHistory = new TreeMap<>(other.dataRateHistory);
        this.dataProtocolHistory = new TreeMap<>(other.dataProtocolHistory);
    }

    public void step(final Duration delta){
        this.now = this.now.plus(delta);
    }

    public void accumulateDataRate(final double delta) {
        this.dataRateHistory.put(this.now, this.dataRateHistory.lastEntry().getValue() + delta);

        if (this.dataRateHistory.lastEntry().getValue() > 100
            && !Objects.equals(this.dataProtocolHistory.lastEntry().getValue(), Protocol.Spacewire))
        {
            setDataProtocol(Protocol.Spacewire);
        }
    }

    public void setDataProtocol(final Protocol protocol) {
        this.dataProtocolHistory.put(this.now, protocol);
    }


    public double getDataRate(){
        return this.dataRateHistory.lastEntry().getValue();
    }

    public double getDataVolume(){
        var volume = this.initialDataVolume;
        var rate = 0.0;

        var now = this.initialInstant;
        for (final var entry : this.dataRateHistory.entrySet()) {
            final var dt = now.durationTo(entry.getKey()).asIntegerQuantity(TimeUnit.SECONDS);
            now = entry.getKey();
            volume += rate * dt;

            rate = entry.getValue();
        }

        if (now.isBefore(this.now)) {
            final var dt = now.durationTo(this.now).asIntegerQuantity(TimeUnit.SECONDS);
            volume += rate * dt;
        }

        return volume;
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
